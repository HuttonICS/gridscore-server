package jhi.gridscore.server.pojo;

import java.util.List;

public class Configuration
{
	private String      name;
	private Integer     cols;
	private Integer     rows;
	private List<Trait> traits;
	private Double[][]  cornerPoints;
	private Cell[][]    data;
	private Brapi       brapiConfig;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Integer getCols()
	{
		return cols;
	}

	public void setCols(Integer cols)
	{
		this.cols = cols;
	}

	public Integer getRows()
	{
		return rows;
	}

	public void setRows(Integer rows)
	{
		this.rows = rows;
	}

	public List<Trait> getTraits()
	{
		return traits;
	}

	public void setTraits(List<Trait> traits)
	{
		this.traits = traits;
	}

	public Double[][] getCornerPoints()
	{
		return cornerPoints;
	}

	public void setCornerPoints(Double[][] cornerPoints)
	{
		this.cornerPoints = cornerPoints;
	}

	public Cell[][] getData()
	{
		return data;
	}

	public void setData(Cell[][] data)
	{
		this.data = data;
	}

	public Brapi getBrapiConfig()
	{
		return brapiConfig;
	}

	public void setBrapiConfig(Brapi brapiConfig)
	{
		this.brapiConfig = brapiConfig;
	}
}
