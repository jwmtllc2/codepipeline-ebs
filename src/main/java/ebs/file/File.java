package ebs.file;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Path("")
public class File {
	private static final Logger LOGGER = Logger.getLogger("");
	


	private static final String BUCKET_NAME = "mtllc-share";

	@Context
	HttpServletRequest request;

	@GET
	@Path("ls/{param}")
	public Response ls(@PathParam("param") String msg) {
		String output = "Jersey file/ls : " + msg;

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain())
				.build();

		ObjectListing listing = s3Client.listObjects(BUCKET_NAME, "");
		List<S3ObjectSummary> summaries = listing.getObjectSummaries();

		while (listing.isTruncated()) {
			listing = s3Client.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getObjectSummaries());
		}

		String result = "<br>Files in " + BUCKET_NAME + ": " + summaries.size() + "<br><br>";
		for (S3ObjectSummary summary : summaries) {
			result += summary.getKey() + "<br>";
		}

		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("cat/{param}")
	public Response cat(@PathParam("param") String msg) {

		try {

			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withCredentials(new DefaultAWSCredentialsProviderChain()).build();
			
			

			S3Object s3object = s3Client.getObject(new GetObjectRequest(BUCKET_NAME, msg));
			System.out.println(s3object.getObjectMetadata().getContentType());
			System.out.println(s3object.getObjectMetadata().getContentLength());

			BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent()));
			String line;
			String content = BUCKET_NAME + "/" + "msg:size=" + s3object.getObjectMetadata().getContentLength() + "<br><br>";
			while ((line = reader.readLine()) != null) {
				// can copy the content locally as well
				// using a buffered writer
				content += line + "<br>";
			}
			
			return Response.status(200).entity(content).build();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "AFTP:" + e.getMessage(), e);
			
			String output = "Unable to cat file: " + BUCKET_NAME + "/" + msg + "<br>";
			output += e.getMessage() + "<br>";
			return Response.status(200).entity(output).build();
		}

		

	}

	@GET
	@Path("{param}")
	public Response usage(@PathParam("param") String msg) {

		if (msg.equals("ls")) {
			return ls("");
		}

		

		String base = request.getRequestURL().substring(0,
				request.getRequestURL().length() - request.getRequestURI().length());

		String output = "<br><br>USAGE:<br>";
		output += "<br><br><h1>" + base + "</h1><br><br>";
		output += "<h3>";
		output += "<br>" + base + "/ls";
		output += "<br>" + base + "/cat/file_name";
		output += "</h3>";
		
		

		return Response.status(200).entity(output).build();
	}

}