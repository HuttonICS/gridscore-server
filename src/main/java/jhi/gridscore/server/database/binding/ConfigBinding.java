package jhi.gridscore.server.database.binding;

import com.google.gson.Gson;
import jhi.gridscore.server.pojo.Configuration;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.sql.*;
import java.util.Objects;

public class ConfigBinding implements Binding<Object, Configuration>
{
	@Override
	public Converter<Object, Configuration> converter()
	{
		Gson gson = new Gson();
		return new Converter<>()
		{
			@Override
			public Configuration from(Object o)
			{
				return o == null ? null : gson.fromJson(Objects.toString(o), Configuration.class);
			}

			@Override
			public Object to(Configuration config)
			{
				return config == null ? null : gson.toJson(config);
			}

			@Override
			public Class<Object> fromType()
			{
				return Object.class;
			}

			@Override
			public Class<Configuration> toType()
			{
				return Configuration.class;
			}
		};
	}

	@Override
	public void sql(BindingSQLContext<Configuration> ctx)
	{
		// Depending on how you generate your SQL, you may need to explicitly distinguish
		// between jOOQ generating bind variables or inlined literals.
		if (ctx.render().paramType() == ParamType.INLINED)
			ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("");
		else
			ctx.render().sql("?");
	}

	@Override
	public void register(BindingRegisterContext<Configuration> ctx)
		throws SQLException
	{
		ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
	}

	@Override
	public void set(BindingSetStatementContext<Configuration> ctx)
		throws SQLException
	{
		ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
	}

	@Override
	public void set(BindingSetSQLOutputContext<Configuration> ctx)
		throws SQLException
	{
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void get(BindingGetResultSetContext<Configuration> ctx)
		throws SQLException
	{
		ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
	}

	@Override
	public void get(BindingGetStatementContext<Configuration> ctx)
		throws SQLException
	{
		ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
	}

	@Override
	public void get(BindingGetSQLInputContext<Configuration> ctx)
		throws SQLException
	{
		throw new SQLFeatureNotSupportedException();
	}
}
