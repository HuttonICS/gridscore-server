package jhi.gridscore.server.util;

import com.google.gson.*;
import jhi.gridscore.server.pojo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import org.jooq.tools.StringUtils;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.stream.IntStream;

public class DataToSpreadsheet
{
	public static void export(File template, File target, Configuration conf, List<MultiTraitAgg> multiTraitAgg)
		throws IOException
	{
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat SDFNS = new SimpleDateFormat("yyyyMMdd");

		try (FileInputStream is = new FileInputStream(template);
			 FileOutputStream os = new FileOutputStream(target))
		{
			List<Cell> cells = new ArrayList<>();
			for (Cell[] r : conf.getData())
				cells.addAll(Arrays.asList(r));

			XSSFWorkbook workbook = new XSSFWorkbook(is);

			XSSFSheet data = workbook.getSheet("DATA");
			XSSFSheet dates = workbook.getSheet("RECORDING_DATES");

			// Write title and description
			XSSFSheet metadata = workbook.getSheet("METADATA");
			metadata.getRow(1).getCell(2).setCellValue(conf.getName());
			metadata.getRow(2).getCell(2).setCellValue("GridScore trial: " + conf.getName());
			if (conf.getLastUpdatedOn() != null)
				metadata.getRow(4).getCell(2).setCellValue(SDF.format(conf.getLastUpdatedOn()));
			else
				metadata.getRow(4).getCell(2).setCellValue(SDF.format(new Date(System.currentTimeMillis())));

			writeTraits(workbook, conf);

			XSSFRow dataRow = data.getRow(0);
			XSSFRow dateRow = dates.getRow(0);
			IntStream.range(0, conf.getTraits().size())
					 .forEach(i -> {
						 Trait t = conf.getTraits().get(i);
						 dataRow.createCell(i + 8).setCellValue(t.getName());
						 dateRow.createCell(i + 8).setCellValue(t.getName());
					 });

			Gson gson = new Gson();

			IntStream.range(0, cells.size())
					 .forEach(i -> {
						 XSSFRow d = data.getRow(i + 1);
						 if (d == null)
							 d = data.createRow(i + 1);
						 XSSFRow p = dates.getRow(i + 1);
						 if (p == null)
							 p = dates.createRow(i + 1);

						 Cell c = cells.get(i);
						 // Write the germplasm name
						 XSSFCell dc = getCell(d, 0);
						 XSSFCell pc = getCell(p, 0);
						 dc.setCellValue(c.getName());
						 pc.setCellValue(c.getName());

						 // Write the location
						 if (c.getGeolocation() != null && c.getGeolocation().getLat() != null && c.getGeolocation().getLng() != null)
						 {
							 Double lat = c.getGeolocation().getLat();
							 Double lng = c.getGeolocation().getLng();
							 Double elv = c.getGeolocation().getElv();

							 dc = getCell(d, 5);
							 pc = getCell(p, 5);
							 dc.setCellValue(lat);
							 pc.setCellValue(lat);
							 dc = getCell(d, 6);
							 pc = getCell(p, 6);
							 dc.setCellValue(lng);
							 pc.setCellValue(lng);

							 if (elv != null)
							 {
								 dc = getCell(d, 7);
								 pc = getCell(p, 7);
								 dc.setCellValue(elv);
								 pc.setCellValue(elv);
							 }
						 }

						 for (int j = 0; j < conf.getTraits().size(); j++)
						 {
							 Trait t = conf.getTraits().get(j);

							 MultiTraitAgg agg = multiTraitAgg.get(j);

							 dc = getCell(d, j + 8);
							 pc = getCell(p, j + 8);

							 String value = c.getValues().get(j);
							 try
							 {
								 // Try to parse the value as an array (will fail for single-traits)
								 String[] valueArray = gson.fromJson(value, String[].class);

								 // If there is no value, set the cell to null
								 if (valueArray == null || valueArray.length < 1)
								 {
									 setCell(t, dc, null);
								 }
								 else
								 {
									 // Else, check the aggregation method
									 if (agg == null)
									 {
										 setCell(t, dc, value);
									 }
									 else if (agg == MultiTraitAgg.last)
									 {
										 setCell(t, dc, valueArray[valueArray.length - 1]);
									 }
									 else if (agg == MultiTraitAgg.avg || agg == MultiTraitAgg.sum)
									 {
										 double total = 0;

										 int count = 0;
										 for (String s : valueArray)
										 {
											 try
											 {
												 total += Double.parseDouble(s);
												 count++;
											 }
											 catch (NullPointerException | NumberFormatException e)
											 {
												 // Do nothing here
											 }
										 }

										 if (agg == MultiTraitAgg.avg)
											 total /= count;

										 setCell(t, dc, Double.toString(total));
									 }
								 }
							 }
							 catch (JsonSyntaxException | NullPointerException e)
							 {
								 setCell(t, dc, value);
							 }

							 try
							 {
								 String date = c.getDates().get(j);
								 if (!StringUtils.isEmpty(value) && !StringUtils.isEmpty(date))
								 {
									 try
									 {
										 String[] dateArray = gson.fromJson(date, String[].class);

										 if (dateArray != null && dateArray.length > 0)
										 {
											 // Use the last date available
											 setCell(t, pc, SDFNS.format(SDF.parse(dateArray[dateArray.length - 1])));
										 }
									 }
									 catch (JsonSyntaxException e)
									 {
										 setCell(t, pc, SDFNS.format(SDF.parse(date)));
									 }
								 }
								 else
								 {
									 setCell(t, pc, null);
								 }
							 }
							 catch (ParseException e)
							 {
								 // Ignore this
							 }
						 }
					 });

			workbook.setActiveSheet(0);
			workbook.write(os);
			workbook.close();
		}
	}

	private static XSSFCell getCell(XSSFRow row, int index)
	{
		XSSFCell cell = row.getCell(index);
		if (cell == null)
			cell = row.createCell(index);
		return cell;
	}

	private static void writeTraits(XSSFWorkbook workbook, Configuration conf)
	{
		XSSFSheet phenotypes = workbook.getSheet("PHENOTYPES");
		XSSFTable traitTable = phenotypes.getTables().get(0);

		// Adjust the table size
		AreaReference area = new AreaReference(traitTable.getStartCellReference(), new CellReference(conf.getTraits().size(), traitTable.getEndCellReference().getCol()), SpreadsheetVersion.EXCEL2007);
		traitTable.setArea(area);
		traitTable.getCTTable().getAutoFilter().setRef(area.formatAsString());
		traitTable.updateReferences();

		final XSSFSheet sheet = traitTable.getXSSFSheet();

		IntStream.range(0, conf.getTraits().size())
				 .forEach(i -> {
					 Trait t = conf.getTraits().get(i);
					 XSSFRow row = sheet.getRow(i + 1);

					 if (row == null)
						 row = sheet.createRow(i + 1);

					 row.createCell(0).setCellValue(t.getName());
					 switch (t.getType())
					 {
						 case "int":
						 case "float":
							 row.createCell(3).setCellValue("numeric");
							 break;
						 case "date":
						 case "text":
						 case "categorical":
							 row.createCell(3).setCellValue(t.getType());
							 break;
						 default:
							 row.createCell(3).setCellValue("text");
							 break;
					 }
					 if (t.getRestrictions() != null)
					 {
						 if (!CollectionUtils.isEmpty(t.getRestrictions().getCategories()))
							 row.createCell(7).setCellValue(String.join(",", t.getRestrictions().getCategories()));
						 if (t.getRestrictions().getMin() != null)
							 row.createCell(8).setCellValue(t.getRestrictions().getMin());
						 if (t.getRestrictions().getMax() != null)
							 row.createCell(9).setCellValue(t.getRestrictions().getMax());
					 }
				 });
	}

	private static void setCell(Trait t, XSSFCell cell, String value)
	{
		if (Objects.equals(t.getType(), "date"))
			cell.setCellType(CellType.STRING);
		cell.setCellValue(value);
	}

	public static enum MultiTraitAgg
	{
		last,
		avg,
		sum
	}
}
