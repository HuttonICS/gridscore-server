package jhi.gridscore.server.pojo;

import java.util.*;
import java.util.stream.Collectors;

public class Cell
{
	private String       name;
	private String       rep;
	private String       brapiId;
	private List<String> dates;
	private List<String> values;
	private Boolean      isMarked;
	private Geolocation  geolocation;
	private String       comment;

	public Cell()
	{
	}

	public Cell(Cell original)
	{
		this.name = original.getName();
		this.rep = original.getRep();
		this.brapiId = original.getBrapiId();
		this.dates = copyList(original.dates);
		this.values = copyList(original.values);
		this.isMarked = original.isMarked;
		this.geolocation = original.geolocation;
		this.comment = original.comment;
	}

	public String getName()
	{
		return name;
	}

	public Cell setName(String name)
	{
		this.name = name;
		return this;
	}

	public String getRep()
	{
		return rep;
	}

	public Cell setRep(String rep)
	{
		this.rep = rep;
		return this;
	}

	public String getBrapiId()
	{
		return brapiId;
	}

	public Cell setBrapiId(String brapiId)
	{
		this.brapiId = brapiId;
		return this;
	}

	public List<String> getDates()
	{
		return dates;
	}

	public Cell setDates(List<String> dates)
	{
		this.dates = dates;
		return this;
	}

	public List<String> getValues()
	{
		return values;
	}

	public Boolean getIsMarked()
	{
		return isMarked;
	}

	public Cell setIsMarked(Boolean isMarked)
	{
		this.isMarked = isMarked;
		return this;
	}

	public Cell setValues(List<String> values)
	{
		this.values = values;
		return this;
	}

	public Geolocation getGeolocation()
	{
		return geolocation;
	}

	public Cell setGeolocation(Geolocation geolocation)
	{
		this.geolocation = geolocation;
		return this;
	}

	public String getComment()
	{
		return comment;
	}

	public Cell setComment(String comment)
	{
		this.comment = comment;
		return this;
	}

	private static List<String> copyList(List<String> input)
	{
		if (input == null)
			return null;
		else
			return input.stream().map(s -> s == null ? null : new String(s)).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Cell cell = (Cell) o;
		return name.equals(cell.name) && dates.equals(cell.dates) && values.equals(cell.values) && Objects.equals(rep, cell.rep) && Objects.equals(isMarked, cell.isMarked) && Objects.equals(geolocation, cell.geolocation) && Objects.equals(comment, cell.comment);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, rep, dates, values, isMarked, geolocation, comment);
	}

	@Override
	public String toString()
	{
		return "Cell{" +
			"name='" + name + '\'' +
			", rep='" + rep + '\'' +
			", dates=" + dates +
			", values=" + values +
			", isMarked=" + isMarked +
			", geolocation=" + geolocation +
			", comment='" + comment + '\'' +
			'}';
	}
}
