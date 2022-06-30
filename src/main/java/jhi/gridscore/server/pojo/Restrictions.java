package jhi.gridscore.server.pojo;

import java.util.*;

public class Restrictions
{
	private Double       min;
	private Double       max;
	private List<String> categories;

	public Double getMin()
	{
		return min;
	}

	public Restrictions setMin(Double min)
	{
		this.min = min;
		return this;
	}

	public Double getMax()
	{
		return max;
	}

	public Restrictions setMax(Double max)
	{
		this.max = max;
		return this;
	}

	public List<String> getCategories()
	{
		return categories;
	}

	public Restrictions setCategories(List<String> categories)
	{
		this.categories = categories;
		return this;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Restrictions that = (Restrictions) o;
		return Objects.equals(min, that.min) && Objects.equals(max, that.max) && Objects.equals(categories, that.categories);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(min, max, categories);
	}
}
