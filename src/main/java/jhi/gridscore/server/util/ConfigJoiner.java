package jhi.gridscore.server.util;

import com.google.gson.Gson;
import jhi.gridscore.server.pojo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.tools.StringUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConfigJoiner
{
	/**
	 * Checks two {@link Configuration}s against each other and checks for updates.
	 * {@link Configuration} a will be updated with any new data points recorded in {@link Configuration} b.
	 *
	 * @param a The "old" configuration that's stored in the database
	 * @param b The "new" configuration as sent by the client
	 * @return <code>true</code> if {@link Configuration} a has been modified due to changes in b, <code>false</code> otherwise.
	 * @throws IncompatibleConfigurationsException Thrown if the provided configurations are incompatible
	 * @throws PriorityDecisionRequiredException   Thrown if there's a data value conflict and no priority has been specified
	 */
	public static Configuration join(Configuration a, Configuration b, KeepPriority priority)
		throws IncompatibleConfigurationsException, PriorityDecisionRequiredException
	{
		// Synchronize on the UUID
		synchronized (a.getUuid())
		{
			// If the names, rows, cols, traits or UUIDs don't match, throw an exception
			if (!Objects.equals(a.getName(), b.getName()) || (a.getDatasetType() != DatasetType.SURVEY && !Objects.equals(a.getRows(), b.getRows())) || !Objects.equals(a.getCols(), b.getCols()) || !Objects.equals(a.getUuid(), b.getUuid()))
				throw new IncompatibleConfigurationsException("Incompatible configurations provided.");

			Set<Trait> combinedTraitSet = new LinkedHashSet<>();
			combinedTraitSet.addAll(a.getTraits());
			combinedTraitSet.addAll(b.getTraits());
			List<Trait> combinedTraits = new ArrayList<>(combinedTraitSet);

			// Unify the traits and remember their indices per config and combined
			Map<String, Integer> ati = new HashMap<>();
			Map<String, Integer> bti = new HashMap<>();

			for (int i = 0; i < a.getTraits().size(); i++)
				ati.put(a.getTraits().get(i).getName(), i);
			for (int i = 0; i < b.getTraits().size(); i++)
				bti.put(b.getTraits().get(i).getName(), i);

			if (a.getDatasetType() == DatasetType.SURVEY)
				return handleSurvey(a, b, combinedTraits, ati, bti, priority);
			else
				return handleTrial(a, b, combinedTraits, ati, bti, priority);
		}
	}

	private static Configuration handleSurvey(Configuration a, Configuration b, List<Trait> combinedTraits, Map<String, Integer> ati, Map<String, Integer> bti, KeepPriority priority)
		throws PriorityDecisionRequiredException
	{
		Set<String> combinedGermplasmSet = new LinkedHashSet<>();
		Arrays.stream(a.getData()).forEach(r -> Arrays.stream(r).forEach(c -> combinedGermplasmSet.add(c.getName())));
		Arrays.stream(b.getData()).forEach(r -> Arrays.stream(r).forEach(c -> combinedGermplasmSet.add(c.getName())));
		List<String> combinedGermplasm = new ArrayList<>(combinedGermplasmSet);

		Map<String, Integer> agi = new HashMap<>();
		Map<String, Integer> bgi = new HashMap<>();

		for (int i = 0; i < a.getData().length; i++)
			agi.put(a.getData()[i][0].getName(), i);
		for (int i = 0; i < b.getData().length; i++)
			bgi.put(b.getData()[i][0].getName(), i);

		Configuration result = new Configuration();
		result.setUuid(b.getUuid());
		result.setName(b.getName());
		result.setRows(combinedGermplasm.size());
		result.setCols(b.getCols());
		result.setDatasetType(b.getDatasetType());
		result.setTraits(combinedTraits);

		Cell[][] ad = a.getData();
		Cell[][] bd = b.getData();

		Date aLatestTotal = null;
		Date bLatestTotal = null;

		Cell[][] data = new Cell[combinedGermplasm.size()][1];
		for (int i = 0; i < combinedGermplasm.size(); i++)
		{
			Integer agIndex = agi.get(combinedGermplasm.get(i));
			Integer bgIndex = bgi.get(combinedGermplasm.get(i));

			Cell ac = null;
			Cell bc = null;
			if (agIndex != null)
				ac = ad[agIndex][0];
			if (bgIndex != null)
				bc = bd[bgIndex][0];

			Cell newCell = new Cell();
			newCell.setName(combinedGermplasm.get(i));

			String[] values = new String[combinedTraits.size()];
			String[] dates = new String[combinedTraits.size()];

			Date aLatest = null;
			Date bLatest = null;

			for (int t = 0; t < combinedTraits.size(); t++)
			{
				Trait trait = combinedTraits.get(t);
				Integer aIndex = ati.get(combinedTraits.get(t).getName());
				Integer bIndex = bti.get(combinedTraits.get(t).getName());

				SingleCellResult dateValue;
				if (Objects.equals(trait.getmType(), "multi"))
					dateValue = resolveCellMulti(ac, aIndex, bc, bIndex);
				else
					dateValue = resolveCellSingle(ac, aIndex, bc, bIndex, priority);

				if (dateValue.valueA != null && dateValue.dateA != null && (aLatest == null || dateValue.dateA.after(aLatest)))
					aLatest = dateValue.dateA;
				if (dateValue.valueB != null && dateValue.dateB != null && (bLatest == null || dateValue.dateB.after(bLatest)))
					bLatest = dateValue.dateB;

				dates[t] = dateValue.resolvedDate;
				values[t] = dateValue.resolvedValue;
			}

			if (ac == null)
			{
				newCell.setGeolocation(bc.getGeolocation());
				newCell.setComment(bc.getComment());
				newCell.setIsMarked(bc.getIsMarked());
			}
			else if (bc == null)
			{
				newCell.setGeolocation(ac.getGeolocation());
				newCell.setComment(ac.getComment());
				newCell.setIsMarked(ac.getIsMarked());
			}
			else if (bLatest != null && (aLatest == null || bLatest.equals(aLatest) || bLatest.after(aLatest)))
			{
				if (!Objects.equals(ac.getGeolocation(), bc.getGeolocation()))
					newCell.setGeolocation(bc.getGeolocation());
				else
					newCell.setGeolocation(ac.getGeolocation());
				if (!Objects.equals(ac.getComment(), bc.getComment()))
					newCell.setComment(bc.getComment());
				else
					newCell.setComment(ac.getComment());
				if (!Objects.equals(ac.getIsMarked(), bc.getIsMarked()))
					newCell.setIsMarked(bc.getIsMarked());
				else
					newCell.setIsMarked(ac.getIsMarked());
			}
			else
			{
				newCell.setGeolocation(ac.getGeolocation());
				newCell.setComment(ac.getComment());
				newCell.setIsMarked(ac.getIsMarked());
			}

			if (aLatest != null && (aLatestTotal == null || aLatest.after(aLatestTotal)))
				aLatestTotal = aLatest;
			if (bLatest != null && (bLatestTotal == null || bLatest.after(bLatestTotal)))
				bLatestTotal = bLatest;

			newCell.setDates(Arrays.asList(dates));
			newCell.setValues(Arrays.asList(values));

			data[i][0] = newCell;
		}

		if (aLatestTotal == null || (bLatestTotal != null && bLatestTotal.after(aLatestTotal))) {
			result.setMarkers(b.getMarkers());
			result.setCornerPoints(b.getCornerPoints());
			result.setBrapiConfig(b.getBrapiConfig());
			result.setComment(b.getComment());
		} else {
			result.setMarkers(a.getMarkers());
			result.setCornerPoints(a.getCornerPoints());
			result.setBrapiConfig(a.getBrapiConfig());
			result.setComment(a.getComment());
		}

		result.setData(data);
		result.setLastUpdatedOn(new Timestamp(System.currentTimeMillis()));

		return result;
	}


	private static Configuration handleTrial(Configuration a, Configuration b, List<Trait> combinedTraits, Map<String, Integer> ati, Map<String, Integer> bti, KeepPriority priority)
		throws IncompatibleConfigurationsException, PriorityDecisionRequiredException
	{
		Configuration result = new Configuration();
		result.setUuid(b.getUuid());
		result.setName(b.getName());
		result.setMarkers(b.getMarkers());
		result.setCornerPoints(b.getCornerPoints());
		result.setBrapiConfig(b.getBrapiConfig());
		result.setRows(b.getRows());
		result.setCols(b.getCols());
		result.setBrapiConfig(b.getBrapiConfig());
		result.setDatasetType(b.getDatasetType());
		if (!StringUtils.isEmpty(b.getComment()))
			result.setComment(b.getComment());
		result.setTraits(combinedTraits);

		Cell[][] ad = a.getData();
		Cell[][] bd = b.getData();

		Date aLatestTotal = null;
		Date bLatestTotal = null;

		for (int y = 0; y < ad.length; y++)
		{
			for (int x = 0; x < ad[y].length; x++)
			{
				Cell ac = ad[y][x];
				Cell bc = bd[y][x];

				if (!Objects.equals(ac.getName(), bc.getName()))
					throw new IncompatibleConfigurationsException("Incompatible cell names: Row: " + y + " col: " + x);

				String[] values = new String[combinedTraits.size()];
				String[] dates = new String[combinedTraits.size()];

				Date aLatest = null;
				Date bLatest = null;

				for (int t = 0; t < combinedTraits.size(); t++)
				{
					Trait trait = combinedTraits.get(t);
					Integer aIndex = ati.get(trait.getName());
					Integer bIndex = bti.get(trait.getName());

					if (Objects.equals(trait.getmType(), "multi"))
					{
						SingleCellResult dateValue = resolveCellMulti(ac, aIndex, bc, bIndex);

						if (dateValue.valueA != null && dateValue.dateA != null && (aLatest == null || dateValue.dateA.after(aLatest)))
							aLatest = dateValue.dateA;
						if (dateValue.valueB != null && dateValue.dateB != null && (bLatest == null || dateValue.dateB.after(bLatest)))
							bLatest = dateValue.dateB;

						dates[t] = dateValue.resolvedDate;
						values[t] = dateValue.resolvedValue;
						if (ac.getGeolocation() == null && bc.getGeolocation() != null)
							ac.setGeolocation(bc.getGeolocation());
					}
					else
					{
						SingleCellResult dateValue = resolveCellSingle(ac, aIndex, bc, bIndex, priority);

						if (dateValue.valueA != null && dateValue.dateA != null && (aLatest == null || dateValue.dateA.after(aLatest)))
							aLatest = dateValue.dateA;
						if (dateValue.valueB != null && dateValue.dateB != null && (bLatest == null || dateValue.dateB.after(bLatest)))
							bLatest = dateValue.dateB;

						dates[t] = dateValue.resolvedDate;
						values[t] = dateValue.resolvedValue;
						ac.setGeolocation(dateValue.geolocation);
					}
				}

				if (aLatest != null && (aLatestTotal == null || aLatest.after(aLatestTotal)))
					aLatestTotal = aLatest;
				if (bLatest != null && (bLatestTotal == null || bLatest.after(bLatestTotal)))
					bLatestTotal = bLatest;

				// We need to decide which marking/comment to keep, check whichever config has the latest change and go with that one.
				if (bLatest != null && (aLatest == null || bLatest.equals(aLatest) || bLatest.after(aLatest)))
				{
					if (!Objects.equals(ac.getComment(), bc.getComment()))
						ac.setComment(bc.getComment());
					if (!Objects.equals(ac.getIsMarked(), bc.getIsMarked()))
						ac.setIsMarked(bc.getIsMarked());
				}
				else
				{
					if (StringUtils.isEmpty(ac.getComment()))
						ac.setComment(bc.getComment());
					if (ac.getIsMarked() == null)
						ac.setIsMarked(bc.getIsMarked());
				}

				ac.setDates(Arrays.asList(dates));
				ac.setValues(Arrays.asList(values));
			}
		}

		if (aLatestTotal == null || (bLatestTotal != null && bLatestTotal.after(aLatestTotal))) {
			result.setMarkers(b.getMarkers());
			result.setCornerPoints(b.getCornerPoints());
			result.setBrapiConfig(b.getBrapiConfig());
			result.setComment(b.getComment());
		} else {
			result.setMarkers(a.getMarkers());
			result.setCornerPoints(a.getCornerPoints());
			result.setBrapiConfig(a.getBrapiConfig());
			result.setComment(a.getComment());
		}

		result.setData(a.getData());
		result.setLastUpdatedOn(new Timestamp(System.currentTimeMillis()));

		return result;
	}

	private static SingleCellResult resolveCellMulti(Cell ac, Integer aIndex, Cell bc, Integer bIndex)
	{
		SingleCellResult result = new SingleCellResult();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		Gson gson = new Gson();

		String[] valuesA = (ac == null || aIndex == null || ac.getValues().get(aIndex) == null) ? null : gson.fromJson(ac.getValues().get(aIndex), String[].class);
		String[] datesA = (ac == null || aIndex == null || ac.getDates().get(aIndex) == null) ? null : gson.fromJson(ac.getDates().get(aIndex), String[].class);
		String[] valuesB = (bc == null || bIndex == null || bc.getValues().get(bIndex) == null) ? null : gson.fromJson(bc.getValues().get(bIndex), String[].class);
		String[] datesB = (bc == null || bIndex == null || bc.getDates().get(bIndex) == null) ? null : gson.fromJson(bc.getDates().get(bIndex), String[].class);

		Set<String> dateValues = new TreeSet<>();
		if (valuesA != null && datesA != null)
		{
			for (int i = 0; i < valuesA.length; i++)
			{
				if (!StringUtils.isEmpty(valuesA[i]))
				{
					dateValues.add(datesA[i] + "|" + valuesA[i]);
					try
					{
						Date date = sdf.parse(datesA[i]);
						if (result.dateA == null || date.after(result.dateA))
						{
							result.valueA = valuesA[i];
							result.dateA = date;
						}
					}
					catch (Exception e)
					{
					}
				}
			}
		}
		if (valuesB != null && datesB != null)
		{
			for (int i = 0; i < valuesB.length; i++)
			{
				if (!StringUtils.isEmpty(valuesB[i]))
					dateValues.add(datesB[i] + "|" + valuesB[i]);
				try
				{
					Date date = sdf.parse(datesB[i]);
					if (result.dateB == null || date.after(result.dateB))
					{
						result.valueB = valuesB[i];
						result.dateB = date;
					}
				}
				catch (Exception e)
				{
				}
			}
		}

		List<String> values = new ArrayList<>();
		List<String> dates = new ArrayList<>();

		dateValues.forEach(s -> {
			int index = s.indexOf('|');

			dates.add(s.substring(0, index));
			values.add(s.substring(index + 1));
		});

		if (!CollectionUtils.isEmpty(values))
		{
			result.resolvedValue = gson.toJson(values.toArray(String[]::new));
			result.resolvedDate = gson.toJson(dates.toArray(String[]::new));
		}
		else
		{
			result.resolvedDate = null;
			result.resolvedValue = null;
		}

		return result;
	}

	private static SingleCellResult resolveCellSingle(Cell ac, Integer aIndex, Cell bc, Integer bIndex, KeepPriority priority)
		throws PriorityDecisionRequiredException
	{
		SingleCellResult result = new SingleCellResult();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		if (ac == null)
		{
			try
			{
				result.resolvedValue = bc.getValues().get(bIndex);
			}
			catch (Exception e)
			{
				result.resolvedValue = null;
			}
			try
			{
				result.resolvedDate = bc.getDates().get(bIndex);
			}
			catch (Exception e)
			{
				result.resolvedDate = null;
			}
			result.geolocation = bc.getGeolocation();
			result.valueA = null;
			result.valueB = result.resolvedValue;
			result.dateA = null;
			try
			{
				result.dateB = sdf.parse(result.resolvedDate);
			}
			catch (Exception e)
			{
				result.dateB = null;
			}
		}
		else if (bc == null)
		{
			try
			{
				result.resolvedValue = ac.getValues().get(aIndex);
			}
			catch (Exception e)
			{
				result.resolvedValue = null;
			}
			try
			{
				result.resolvedDate = ac.getDates().get(aIndex);
			}
			catch (Exception e)
			{
				result.resolvedDate = null;
			}
			result.geolocation = ac.getGeolocation();
			result.valueB = null;
			result.valueA = result.resolvedValue;
			result.dateB = null;
			try
			{
				result.dateA = sdf.parse(result.resolvedDate);
			}
			catch (Exception e)
			{
				result.dateA = null;
			}
		}
		else
		{
			try
			{
				result.dateA = sdf.parse(ac.getDates().get(aIndex));
			}
			catch (Exception e)
			{
				result.dateA = null;
			}
			try
			{
				result.valueA = ac.getValues().get(aIndex);
			}
			catch (Exception e)
			{
				result.valueA = null;
			}
			try
			{
				result.dateB = sdf.parse(bc.getDates().get(bIndex));
			}
			catch (Exception e)
			{
				result.dateB = null;
			}
			try
			{
				result.valueB = bc.getValues().get(bIndex);
			}
			catch (Exception e)
			{
				result.valueB = null;
			}

			// Default to A's values
			result.resolvedValue = result.valueA;
			result.resolvedDate = result.dateA == null ? null : sdf.format(result.dateA);
			result.geolocation = ac.getGeolocation();

			if (result.dateA != null && result.dateB != null && Objects.equals(result.dateA, result.dateB) && !Objects.equals(result.valueA, result.valueB))
			{
				if (priority == null)
					throw new PriorityDecisionRequiredException("Incompatible values for cells: " + result.dateA + " " + result.dateB);

				switch (priority)
				{
					case MINE:
						result.resolvedValue = result.valueB;
						try
						{
							result.resolvedDate = sdf.format(result.dateB);
						}
						catch (Exception e)
						{
							result.resolvedDate = null;
						}
						result.geolocation = bc.getGeolocation();
						break;
					case THEIRS:
						result.resolvedValue = result.valueA;
						try
						{
							result.resolvedDate = sdf.format(result.dateA);
						}
						catch (Exception e)
						{
							result.resolvedDate = null;
						}
						break;
				}
			}
			else if (result.dateA == null || (result.dateB != null && (result.dateB.equals(result.dateA) || result.dateB.after(result.dateA))))
			{
				// Check if either `a` doesn't have a value yet (no date) or `b` has been scored at the same time or later
				// B has genuinely new data
				result.resolvedValue = result.valueB;
				try
				{
					result.resolvedDate = sdf.format(result.dateB);
				}
				catch (Exception e)
				{
					result.resolvedDate = null;
				}

				// Check GPS
				if (!Objects.equals(ac.getGeolocation(), bc.getGeolocation()))
					result.geolocation = bc.getGeolocation();
			}
		}

		return result;
	}

	private static class SingleCellResult
	{
		String      resolvedValue;
		String      resolvedDate;
		String      valueA;
		String      valueB;
		Date        dateA;
		Date        dateB;
		Geolocation geolocation;
	}

	public static class IncompatibleConfigurationsException extends Exception
	{
		public IncompatibleConfigurationsException(String message)
		{
			super(message);
		}
	}

	public static class PriorityDecisionRequiredException extends Exception
	{
		public PriorityDecisionRequiredException(String message)
		{
			super(message);
		}
	}
}
