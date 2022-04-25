package jhi.gridscore.server.util;

import jhi.gridscore.server.pojo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.*;
import org.jooq.tools.StringUtils;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.stream.IntStream;

public class DataToSpreadsheet
{
	public static void export(File template, File target, Configuration conf)
		throws IOException
	{
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat SDFNS = new SimpleDateFormat("yyyyMMdd");

		try (FileInputStream is = new FileInputStream(template);
			 FileOutputStream os = new FileOutputStream(target))
		{
			XSSFWorkbook workbook = new XSSFWorkbook(is);

			XSSFSheet data = workbook.getSheet("DATA");
			XSSFSheet dates = workbook.getSheet("RECORDING_DATES");

			// Write title and description
			XSSFSheet metadata = workbook.getSheet("METADATA");
			metadata.getRow(1).getCell(2).setCellValue(conf.getName());
			metadata.getRow(2).getCell(2).setCellValue("GridScore trial: " + conf.getName());
			metadata.getRow(4).getCell(2).setCellValue(SDF.format(conf.getLastUpdatedOn()));

			XSSFSheet phenotypes = workbook.getSheet("PHENOTYPES");
			IntStream.range(0, conf.getTraits().size())
					 .forEach(i -> {
						 Trait t = conf.getTraits().get(i);
						 XSSFRow row = phenotypes.createRow(i + 1);
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

			XSSFRow dataRow = data.getRow(0);
			XSSFRow dateRow = dates.getRow(0);
			IntStream.range(0, conf.getTraits().size())
					 .forEach(i -> {
						 Trait t = conf.getTraits().get(i);
						 dataRow.createCell(i + 4).setCellValue(t.getName());
						 dateRow.createCell(i + 4).setCellValue(t.getName());
					 });

			List<Cell> cells = new ArrayList<>();
			for (Cell[] r : conf.getData())
				cells.addAll(Arrays.asList(r));

			IntStream.range(0, cells.size())
					 .forEach(i -> {
						 XSSFRow d = data.createRow(i + 1);
						 XSSFRow p = dates.createRow(i + 1);
						 Cell c = cells.get(i);
						 // Write the germplasm name
						 d.createCell(0).setCellValue(c.getName());
						 p.createCell(0).setCellValue(c.getName());

						 // Write the data
						 IntStream.range(0, conf.getTraits().size())
								  .forEach(j -> {
									  Trait t = conf.getTraits().get(j);

									  setCell(t, d.createCell(j + 4), c.getValues().get(j));
									  try
									  {
										  if (!StringUtils.isEmpty(c.getValues().get(j)))
											  setCell(t, p.createCell(j + 4), SDFNS.format(SDF.parse(c.getDates().get(j))));
									  }
									  catch (ParseException e)
									  {
										  // Ignore this
									  }
								  });
					 });

			workbook.setActiveSheet(0);
			workbook.write(os);
		}
	}

	private static void setCell(Trait t, XSSFCell cell, String value)
	{
		if (Objects.equals(t.getType(), "date"))
			cell.setCellType(CellType.STRING);
		cell.setCellValue(value);
	}
}
