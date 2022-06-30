package jhi.gridscore.server.pojo;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Markers markers = (Markers) o;
		return Objects.equals(corner, markers.corner) && Objects.equals(everyRow, markers.everyRow) && Objects.equals(everyCol, markers.everyCol);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(corner, everyRow, everyCol);
	}
}
