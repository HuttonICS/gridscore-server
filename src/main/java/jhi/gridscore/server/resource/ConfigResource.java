package jhi.gridscore.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.PropertyWatcher;
import jhi.gridscore.server.database.Database;
import jhi.gridscore.server.database.codegen.tables.pojos.Configurations;
import jhi.gridscore.server.database.codegen.tables.records.ConfigurationsRecord;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.util.*;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

@Path("config")
public class ConfigResource extends ContextResource
{
	private static final SecureRandom   RANDOM  = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String postConfiguration(Configuration conf)
		throws IOException, SQLException
	{
		if (conf == null)
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return null;
		}
		else
		{
			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				// Has an id been provided?
				if (!StringUtils.isEmpty(conf.getUuid()))
				{
					// Get the old config
					ConfigurationsRecord dbConf = context.selectFrom(CONFIGURATIONS).where(CONFIGURATIONS.UUID.eq(conf.getUuid())).fetchAny();

					if (dbConf != null)
					{
						// Update it
						dbConf.setConfiguration(conf);
						dbConf.setCreatedOn(new Timestamp(System.currentTimeMillis()));
						dbConf.store();

						// Return the same id
						return conf.getUuid();
					}
					else
					{
						// Doesn't exist, create it
						return addNewConfig(context, conf);
					}
				}
				else
				{
					// No id provided, create it
					return addNewConfig(context, conf);
				}
			}
		}
	}

	@GET
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Configuration getConfiguration(@PathParam("id") String id)
		throws SQLException, IOException
	{
		if (StringUtils.isEmpty(id))
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return null;
		}
		else
		{
			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				ConfigurationsRecord record = context.selectFrom(CONFIGURATIONS)
													 .where(CONFIGURATIONS.UUID.eq(id))
													 .fetchAny();

				if (record == null)
				{
					resp.sendError(Response.Status.NOT_FOUND.getStatusCode());
					return null;
				}
				else
				{
					Configuration result = record.getConfiguration();
					result.setUuid(id);

					return result;
				}
			}
		}
	}

	@GET
	@Path("/{id}/export-g8")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getConfigExportUuid(@PathParam("id") String id)
		throws IOException, SQLException
	{
		if (StringUtils.isEmpty(id))
		{
			resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
			return null;
		}
		else
		{
			try (Connection conn = Database.getConnection();
				 DSLContext context = Database.getContext(conn))
			{
				ConfigurationsRecord record = context.selectFrom(CONFIGURATIONS)
													 .where(CONFIGURATIONS.UUID.eq(id))
													 .fetchAny();

				if (record == null)
				{
					resp.sendError(Response.Status.NOT_FOUND.getStatusCode());
					return null;
				}
				else
				{
					Configuration result = record.getConfiguration();

					URL resource = PropertyWatcher.class.getClassLoader().getResource("trials-data.xlsx");
					if (resource != null)
					{
						File template = new File(resource.toURI());

						String uuid = UUID.randomUUID().toString();
						File folder = new File(System.getProperty("java.io.tmpdir"), "gridscore");
						folder.mkdirs();
						File sourceCopy = new File(folder, "template-" + uuid + ".xlsx");
						Files.copy(template.toPath(), sourceCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
						File target = new File(folder, uuid + ".xlsx");

						DataToSpreadsheet.export(sourceCopy, target, result);

						sourceCopy.delete();

						return uuid;
					}
				}
			}
			catch (URISyntaxException e)
			{
				// Template file wasn't found
				e.printStackTrace();
				Logger.getLogger("").severe(e.getMessage());
				resp.sendError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
				return null;
			}
		}

		return null;
	}

	@GET
	@Path("/{id}/export-g8/{uuid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public Response getConfigExportDownload(@PathParam("id") String configId, @PathParam("uuid") String uuid)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			Configurations record = context.selectFrom(CONFIGURATIONS)
												 .where(CONFIGURATIONS.UUID.eq(configId))
												 .fetchAnyInto(Configurations.class);

			File folder = new File(System.getProperty("java.io.tmpdir"), "gridscore");
			File result = new File(folder, uuid + ".xlsx");

			if (!FileUtils.isSubDirectory(folder, result))
			{
				resp.sendError(Response.Status.BAD_REQUEST.getStatusCode());
				return null;
			}
			if (!result.exists() || !result.isFile()) {
				resp.sendError(Response.Status.NOT_FOUND.getStatusCode());
				return null;
			}

			String friendlyFilename = record.getConfiguration().getName().replaceAll("\\W+", "-") + "-" + record.getUuid();

			java.nio.file.Path zipFilePath = result.toPath();
			return Response.ok((StreamingOutput) output -> {
							   Files.copy(zipFilePath, output);
							   Files.deleteIfExists(zipFilePath);
						   })
						   .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
						   .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + friendlyFilename + ".xlsx\"")
						   .header(HttpHeaders.CONTENT_LENGTH, result.length())
						   .build();
		}
	}

	private String addNewConfig(DSLContext context, Configuration conf)
	{
		byte[] buffer = new byte[20];
		RANDOM.nextBytes(buffer);
		String id = ENCODER.encodeToString(buffer);

		ConfigurationsRecord record = context.newRecord(CONFIGURATIONS);
		record.setUuid(id);
		record.setConfiguration(conf);
		record.store();

		return id;
	}
}
