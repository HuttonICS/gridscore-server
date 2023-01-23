package jhi.gridscore.server.resource;

import com.google.gson.Gson;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.PropertyWatcher;
import jhi.gridscore.server.database.Database;
import jhi.gridscore.server.database.codegen.tables.pojos.Configurations;
import jhi.gridscore.server.database.codegen.tables.records.ConfigurationsRecord;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import jhi.gridscore.server.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.*;
import org.geotools.data.simple.*;
import org.geotools.feature.simple.*;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static jhi.gridscore.server.database.codegen.tables.Configurations.*;

@Path("config")
public class ConfigResource extends ContextResource
{
	private static final SecureRandom   RANDOM  = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	@POST
	@Path("/checkupdate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postConfigUpdates(List<MiniConf> toCheck)
		throws SQLException
	{
		if (CollectionUtils.isEmpty(toCheck))
			return Response.ok(new ArrayList<>()).build();

		List<String> uuids = toCheck.stream()
									.map(MiniConf::getUuid)
									.filter(Objects::nonNull)
									.collect(Collectors.toList());

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			Map<String, ConfigurationsRecord> matches = context.selectFrom(CONFIGURATIONS)
															   .where(CONFIGURATIONS.UUID.in(uuids))
															   .fetchMap(CONFIGURATIONS.UUID);

			// Return the matches (based on UUID) where there exists a request with the same UUID and an older updated date
			return Response.ok(toCheck.stream()
									  .map(t -> {
										  ConfigurationsRecord match = matches.get(t.getUuid());

										  if (match != null && match.getConfiguration().getLastUpdatedOn() != null && t.getLastUpdatedOn() != null)
											  return match.getConfiguration().getLastUpdatedOn().after(t.getLastUpdatedOn());
										  else
											  return false;
									  })
									  .collect(Collectors.toList())).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postConfiguration(Configuration conf, @QueryParam("override") boolean override, @QueryParam("priority") KeepPriority priority)
		throws IOException, SQLException
	{
		if (conf == null)
		{
			return Response.status(Response.Status.BAD_REQUEST).build();
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
					ConfigurationsRecord dbConf = context.selectFrom(CONFIGURATIONS)
														 .where(CONFIGURATIONS.UUID.eq(conf.getUuid()))
														 .fetchAny();

					if (dbConf != null)
					{
						if (override)
						{
							dbConf.setConfiguration(conf);
							dbConf.setCreatedOn(new Timestamp(System.currentTimeMillis()));
							dbConf.store();

							return Response.ok(conf).build();
						}
						else
						{
							try
							{
								Configuration res = ConfigJoiner.join(dbConf.getConfiguration(), conf, priority);
								dbConf.setConfiguration(res);
								dbConf.setCreatedOn(new Timestamp(System.currentTimeMillis()));
								dbConf.store();

								return Response.ok(res).build();
							}
							catch (ConfigJoiner.IncompatibleConfigurationsException e)
							{
								return Response.status(Response.Status.CONFLICT.getStatusCode(), "Incompatible configurations found. Ask for user override.").build();
							}
							catch (ConfigJoiner.PriorityDecisionRequiredException e)
							{
								return Response.status(Response.Status.PRECONDITION_REQUIRED.getStatusCode(), "Cannot join configurations, user decision required regarding priority.").build();
							}
						}

						// Update it
//						dbConf.setConfiguration(conf);
//						dbConf.setCreatedOn(new Timestamp(System.currentTimeMillis()));
//						dbConf.store();

						// Return the same id
//						return Response.ok(conf.getUuid()).build();
					}
					else
					{
						// Doesn't exist, create it
						addNewConfig(context, conf);
						return Response.ok(conf).build();
					}
				}
				else
				{
					// No id provided, create it
					addNewConfig(context, conf);
					return Response.ok(conf).build();
				}
			}
		}
	}

	@GET
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfiguration(@PathParam("id") String id)
		throws SQLException, IOException
	{
		if (StringUtils.isEmpty(id))
		{
			return Response.status(Response.Status.BAD_REQUEST)
						   .build();
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
					return Response.status(Response.Status.NOT_FOUND)
								   .build();
				}
				else
				{
					Configuration result = record.getConfiguration();
					result.setUuid(id);

					return Response.ok(result)
								   .build();
				}
			}
		}
	}

	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteConfiguration(@PathParam("id") String id, @QueryParam("name") String name)
		throws SQLException
	{
		if (StringUtils.isEmpty(name) || StringUtils.isEmpty(id))
			return Response.status(Response.Status.BAD_REQUEST).build();

		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			ConfigurationsRecord record = context.selectFrom(CONFIGURATIONS)
												 .where(CONFIGURATIONS.UUID.eq(id))
												 .fetchAny();

			if (Objects.equals(record.getConfiguration().getName(), name))
			{
				record.delete();
				return Response.ok().build();
			}
			else
			{
				return Response.status(Response.Status.FORBIDDEN).build();
			}
		}
	}

	@POST
	@Path("/{id}/export-shapefile")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postConfigExportShapefile(@PathParam("id") String id, ShapefileConfig config)
		throws SQLException, IOException
	{
		if (StringUtils.isEmpty(id))
		{
			return Response.status(Response.Status.BAD_REQUEST)
						   .build();
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
					return Response.status(Response.Status.NOT_FOUND)
								   .build();
				}
				else
				{
					Configuration result = record.getConfiguration();

					final SimpleFeatureType TYPE = createFeatureType();

					List<SimpleFeature> features = new ArrayList<>();

					GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
					SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

					for (int row = 0; row < result.getRows(); row++) {
						for (int col = 0; col < result.getCols(); col++) {
							Cell cell = result.getData()[row][col];

							Coordinate[] coordinates = getCoordinates(config, row, col);
//							Coordinate[] coordinates = new Coordinate[]{new Coordinate(-3.031263, 56.500166), new Coordinate(-3.031020, 56.497888), new Coordinate(-3.025255, 56.497921), new Coordinate(-3.025619, 56.500266), new Coordinate(-3.031263, 56.500166)};

							Polygon polygon = geometryFactory.createPolygon(coordinates);
							featureBuilder.add(polygon);
							featureBuilder.add(cell.getName());
							featureBuilder.add(row + 1);
							featureBuilder.add(col + 1);
							featureBuilder.add(cell.getRep());
							SimpleFeature feature = featureBuilder.buildFeature(null);
							features.add(feature);
						}
					}

					/*
					 * Get an output file name and create the new shapefile
					 */
					String uuid = UUID.randomUUID().toString();
					File folder = new File(new File(System.getProperty("java.io.tmpdir"), "gridscore"), uuid);
					folder.mkdirs();
					File target = new File(folder, uuid + ".shp");

					ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

					Map<String, Serializable> params = new HashMap<>();
					params.put("url", target.toURI().toURL());
					params.put("create spatial index", Boolean.TRUE);

					ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
					newDataStore.createSchema(TYPE);

					/*
					 * You can comment out this line if you are using the createFeatureType method (at end of
					 * class file) rather than DataUtilities.createType
					 */
					//newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

					/*
					 * Write the features to the shapefile
					 */
					Transaction transaction = new DefaultTransaction("create");

					String typeName = newDataStore.getTypeNames()[0];
					SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

					if (featureSource instanceof SimpleFeatureStore)
					{
						SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

						/*
						 * SimpleFeatureStore has a method to add features from a
						 * SimpleFeatureCollection object, so we use the ListFeatureCollection
						 * class to wrap our list of features.
						 */
						SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
						featureStore.setTransaction(transaction);
						try
						{
							featureStore.addFeatures(collection);
							transaction.commit();

						}
						catch (Exception problem)
						{
							problem.printStackTrace();
							transaction.rollback();

						}
						finally
						{
							transaction.close();
						}
					}

					FileUtils.zipUp(folder);

					return Response.ok(uuid).build();
				}
			}
			catch (MalformedURLException e)
			{
				// Template file wasn't found
				e.printStackTrace();
				Logger.getLogger("").severe(e.getMessage());
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
							   .build();
			}
		}
	}

	@GET
	@Path("/{id}/export-shapefile/{uuid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/zip")
	public Response getConfigShapefileDownload(@PathParam("id") String configId, @PathParam("uuid") String uuid)
		throws IOException, SQLException
	{
		try (Connection conn = Database.getConnection();
			 DSLContext context = Database.getContext(conn))
		{
			Configurations record = context.selectFrom(CONFIGURATIONS)
										   .where(CONFIGURATIONS.UUID.eq(configId))
										   .fetchAnyInto(Configurations.class);

			File folder = new File(new File(System.getProperty("java.io.tmpdir"), "gridscore"), uuid);
			File result = new File(folder, uuid + ".zip");

			if (!FileUtils.isSubDirectory(folder, result))
				return Response.status(Response.Status.BAD_REQUEST).build();
			if (!result.exists() || !result.isFile())
				return Response.status(Response.Status.NOT_FOUND).build();

			String friendlyFilename = record.getConfiguration().getName().replaceAll("\\W+", "-") + "-" + record.getUuid();

			java.nio.file.Path zipFilePath = result.toPath();
			return Response.ok((StreamingOutput) output -> {
							   Files.copy(zipFilePath, output);
							   Files.deleteIfExists(zipFilePath);
						   })
						   .type("application/zip")
						   .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + friendlyFilename + ".zip\"")
						   .header(HttpHeaders.CONTENT_LENGTH, result.length())
						   .build();
		}
	}

	private static Coordinate[] getCoordinates(ShapefileConfig config, int row, int col)
	{
		Double[] bl = config.get(col + "-" + row);
		Double[] br = config.get((col + 1) + "-" + row);
		Double[] tr = config.get((col + 1) + "-" + (row + 1));
		Double[] tl = config.get(col + "-" + (row + 1));

		return new Coordinate[] {new Coordinate(tl[1], tl[0]), new Coordinate(bl[1], bl[0]), new Coordinate(br[1], br[0]), new Coordinate(tr[1], tr[0]), new Coordinate(tl[1], tl[0])};
	}

	private static SimpleFeatureType createFeatureType()
	{

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("Location");
		builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system

		// add attributes in order
		builder.add("the_geom", Polygon.class);
		builder.add("germplasm", String.class); // <- 15 chars width for name field
		builder.add("row", Integer.class);
		builder.add("column", Integer.class);
		builder.add("rep", String.class);

		// build the type
		return builder.buildFeatureType();
	}

	@POST
	@Path("/{id}/export-g8")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigExportUuid(@PathParam("id") String id, List<DataToSpreadsheet.MultiTraitAgg> multiTraitAgg)
		throws IOException, SQLException
	{
		if (StringUtils.isEmpty(id))
		{
			return Response.status(Response.Status.BAD_REQUEST)
						   .build();
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
					return Response.status(Response.Status.NOT_FOUND)
								   .build();
				}
				else
				{
					Configuration result = record.getConfiguration();

					if (!CollectionUtils.isEmpty(multiTraitAgg) && multiTraitAgg.size() != result.getTraits().size())
					{
						return Response.status(Response.Status.BAD_REQUEST)
									   .build();
					}

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

						DataToSpreadsheet.export(sourceCopy, target, result, multiTraitAgg);

						sourceCopy.delete();

						return Response.ok(uuid)
									   .build();
					}
				}
			}
			catch (URISyntaxException e)
			{
				// Template file wasn't found
				e.printStackTrace();
				Logger.getLogger("").severe(e.getMessage());
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
							   .build();
			}
		}

		return Response.noContent().build();
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
				return Response.status(Response.Status.BAD_REQUEST).build();
			if (!result.exists() || !result.isFile())
				return Response.status(Response.Status.NOT_FOUND).build();

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
		String id;

		if (!StringUtils.isEmpty(conf.getUuid()) && conf.getUuid().length() <= 36)
		{
			id = conf.getUuid();
		}
		else
		{
			byte[] buffer = new byte[20];
			RANDOM.nextBytes(buffer);
			id = ENCODER.encodeToString(buffer);
			conf.setUuid(id);
		}

		ConfigurationsRecord record = context.newRecord(CONFIGURATIONS);
		record.setUuid(id);
		record.setConfiguration(conf);
		record.store();

		return id;
	}
}
