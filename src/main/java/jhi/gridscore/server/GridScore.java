package jhi.gridscore.server;

import jhi.gridscore.server.resource.*;
import org.restlet.*;
import org.restlet.data.*;
import org.restlet.engine.application.*;
import org.restlet.resource.ServerResource;
import org.restlet.routing.*;
import org.restlet.service.EncoderService;
import org.restlet.util.Series;

import java.util.*;

public class GridScore extends Application
{
	public GridScore()
	{
		// Set information about API
		setName("GridScore Server");
		setDescription("This is the server implementation for GridScore");
		setOwner("The James Hutton Institute");
		setAuthor("Sebastian Raubach, Information & Computational Sciences");
	}

	@Override
	public Restlet createInboundRoot()
	{
		Context context = getContext();

		// Create new router
		Router router = new Router(context);

		// Set the encoder
		Filter encoder = new Encoder(context, false, true, new EncoderService(true));
		encoder.setNext(router);

		// Set the Cors filter
		CorsFilter corsFilter = new CorsFilter(context, encoder)
		{
			@Override
			protected int beforeHandle(Request request, Response response)
			{
				if (getCorsResponseHelper().isCorsRequest(request))
				{
					Series<Header> headers = request.getHeaders();

					for (Header header : headers)
					{
						if (header.getName().equalsIgnoreCase("origin"))
						{
							response.setAccessControlAllowOrigin(header.getValue());
						}
					}
				}
				return super.beforeHandle(request, response);
			}
		};
		corsFilter.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
		corsFilter.setSkippingResourceForCorsOptions(true);
		corsFilter.setAllowingAllRequestedHeaders(true);
		corsFilter.setDefaultAllowedMethods(new HashSet<>(Arrays.asList(Method.POST, Method.GET, Method.OPTIONS)));
		corsFilter.setAllowedCredentials(true);
		corsFilter.setExposedHeaders(Collections.singleton("Content-Disposition"));

		attachToRouter(router, "/config", ConfigServerResource.class);
		attachToRouter(router, "/config/{uuid}", SpecificConfigServerResource.class);

		return corsFilter;
	}

	private void attachToRouter(Router router, String url, Class<? extends ServerResource> clazz)
	{
		router.attach(url, clazz);
		router.attach(url + "/", clazz);
	}
}
