import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import jhi.gridscore.server.pojo.Configuration;
import jhi.gridscore.server.pojo.*;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.util.StringUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class ConfigTest
{
	protected static String             URL = "http://localhost:8080/gridscore-api/v1.11.0/api/config";
	protected static Client             client;
	protected static Invocation.Builder postBuilder;

	protected void assertConfigEquals(Configuration config, Configuration result)
	{
		Assertions.assertEquals(config.getName(), result.getName());
		Assertions.assertEquals(config.getUuid(), result.getUuid());
		Assertions.assertEquals(config.getRows(), result.getRows());
		Assertions.assertEquals(config.getCols(), result.getCols());
		Assertions.assertEquals(config.getComment(), result.getComment());
		Assertions.assertEquals(config.getTraits(), result.getTraits());
		Assertions.assertArrayEquals(config.getData(), result.getData());
		Assertions.assertEquals(config.getDatasetType(), result.getDatasetType());
		Assertions.assertEquals(config.getBrapiConfig(), result.getBrapiConfig());

//		if (config.getLastUpdatedOn() != null)
//			Assertions.assertTrue(result.getLastUpdatedOn().equals(config.getLastUpdatedOn()) || result.getLastUpdatedOn().after(config.getLastUpdatedOn()));
	}

	protected static void setUpClient()
	{
		client = ClientBuilder.newBuilder()
							  .build();

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX"));
		// Create a Jackson Provider
		JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider(objectMapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
		client.register(jsonProvider);

		postBuilder = client.target(URL)
							.request(MediaType.APPLICATION_JSON);
	}

	protected ApiResult<Configuration> getConfiguration(String uuid)
	{
		WebTarget target = client.target(URL);

		if (!StringUtils.isBlank(uuid))
			target = target.path(uuid);

		Response response = target.request(MediaType.APPLICATION_JSON)
								  .get();

		int code = response.getStatus();
		Configuration result = code == 200 ? response.readEntity(Configuration.class) : null;

		return new ApiResult<Configuration>().setData(result).setStatus(code);
	}

	protected ApiResult<Configuration> sendConfiguration(Configuration config)
	{
		return sendConfiguration(config, null);
	}

	protected ApiResult<Configuration> sendConfiguration(Configuration config, String priority)
	{
		Response response;

		if (StringUtils.isBlank(priority))
			response = postBuilder.post(Entity.entity(config, MediaType.APPLICATION_JSON));
		else
			response = client.target(URL)
							 .queryParam("priority", priority)
							 .request(MediaType.APPLICATION_JSON_TYPE)
							 .post(Entity.entity(config, MediaType.APPLICATION_JSON));

		int code = response.getStatus();
		Configuration result = code == 200 ? response.readEntity(Configuration.class) : null;

		return new ApiResult<Configuration>().setData(result).setStatus(code);
	}

	protected ApiResult<List<Boolean>> checkForUpdate(Configuration config, Timestamp timestamp)
	{
		Response response = client.target(URL)
								  .path("checkupdate")
								  .request(MediaType.APPLICATION_JSON)
								  .post(Entity.entity(Collections.singletonList(new MiniConf().setUuid(config.getUuid()).setLastUpdatedOn(timestamp)), MediaType.APPLICATION_JSON));

		int code = response.getStatus();
		List<Boolean> result = code == 200 ? response.readEntity(new GenericType<>()
		{
		}) : null;

		return new ApiResult<List<Boolean>>().setData(result).setStatus(code);
	}

	protected static class ApiResult<T>
	{
		public T   data;
		public int status;

		public ApiResult<T> setData(T data)
		{
			this.data = data;
			return this;
		}

		public ApiResult<T> setStatus(int status)
		{
			this.status = status;
			return this;
		}
	}
}
