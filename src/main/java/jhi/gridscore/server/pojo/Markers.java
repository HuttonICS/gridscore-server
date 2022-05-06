package jhi.gridscore.server.pojo;

public class Markers
{
	private String corner;
	private Double everyRow;
	private Double everyCol;

	public String getCorner()
	{
		return corner;
	}

	public Markers setCorner(String corner)
	{
		this.corner = corner;
		return this;
	}

	public Double getEveryRow()
	{
		return everyRow;
	}

	public Markers setEveryRow(Double everyRow)
	{
		this.everyRow = everyRow;
		return this;
	}

	public Double getEveryCol()
	{
		return everyCol;
	}

	public Markers setEveryCol(Double everyCol)
	{
		this.everyCol = everyCol;
		return this;
	}
}
