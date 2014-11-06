// BSD License (http://www.galagosearch.org/license)

package org.galagosearch.core.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.galagosearch.core.index.PositionIndexReader.Iterator;
import org.galagosearch.core.parse.Document;
import org.galagosearch.core.tools.Search.SearchResult;
import org.galagosearch.core.tools.Search.SearchResultItem;
import org.galagosearch.tupleflow.Utility;
import org.mortbay.jetty.handler.AbstractHandler;

import org.znerd.xmlenc.XMLOutputter;

import sun.util.locale.StringTokenIterator;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * 
 * 
 * <p>
 * Handles web search requests against a Galago index. Also handles XML requests
 * for documents, snippets and search results.
 * </p>
 * 
 * <p>
 * This class is set up to work with an embedded Jetty instance, but it should
 * be fairly easy to wrap into a Servlet for use with something else (Tomcat,
 * Glassfish, etc.)
 * </p>
 * 
 * <p>
 * URLs supported:
 * </p>
 * 
 * <table>
 * <tr>
 * <td>/</td>
 * <td>Main Page</td>
 * </tr>
 * <tr>
 * <td>/search</td>
 * <td>HTML Search Results (q, start, n)</td>
 * </tr>
 * <tr>
 * <td>/xmlsearch</td>
 * <td>XML Search Results (q, start, n)</td>
 * </tr>
 * <tr>
 * <td>/snippet</td>
 * <td>XML Snippet Result (identifier, term+)</td>
 * </tr>
 * <tr>
 * <td>/document</td>
 * <td>Document Result (identifier)</td>
 * </tr>
 * </table>
 * 
 * @author trevor
 */
public class SearchWebHandler extends AbstractHandler {
	Search search;
	static long startTime, endTime;
	static HashMap<Integer, String> s = new HashMap<Integer, String>();
	static int size;
	public static String value;
	public static String value1;
    public static HashMap<String,Double> precison=new HashMap<String,Double>();
	public static HashMap<Integer,String> val=new HashMap<Integer,String>();
	public static Set<String>Concat=new HashSet<String>();
	public SearchResult result;
	static int po=1;
	static String check="";
    public SearchWebHandler(Search search) {
		this.search = search;
	}

	public String getEscapedString(String text) {
		StringBuilder builder = new StringBuilder();
        
		for (int i = 0; i < text.length(); ++i) {
			char c = text.charAt(i);
			if (c >= 128) {
				builder.append("&#" + (int) c + ";");
			} else {
				builder.append(c);
			}
		}
        
       
		return builder.toString();
	}

	public void handleDocument(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		request.getParameterMap();
		String identifier = request.getParameter("identifier");
		Document document = search.getDocument(identifier);
		response.setContentType("text/html; charset=UTF-8");
		int kl=0;
		for (SearchResultItem item : result.items) {
			  
			if(item.identifier.equals(identifier))
			{    kl=item.rank;
			     if(!precison.containsKey(identifier))
			     {
				 precison.put(identifier,(double)po/item.rank);
				 System.out.println("Precison of document ranked "+kl+" have value  "+precison.get(identifier));
			     }
				 po++;
				 break;
				
			}
		}
        
		double ap=0;
        for(Map.Entry<String,Double> vk:precison.entrySet())
		{
			ap+=vk.getValue();
		}
        System.out.println("Average Precison of overall clicked is "+ap/precison.size());
        PrintWriter writer = response.getWriter();
        String human=getEscapedString(document.text);
       
		writer.write(human);
        
       
		//System.out.println(getEscapedString(document.text));
		writer.close();
	}

	public void handleSnippet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		String identifier = request.getParameter("identifier");
		String[] terms = request.getParameterValues("term");
		Set<String> queryTerms = new HashSet<String>(Arrays.asList(terms));

		Document document = search.getDocument(identifier);

		if (document == null) {
			response.setStatus(response.SC_NOT_FOUND);
		} else {
			response.setContentType("text/xml");
			PrintWriter writer = response.getWriter();
			String snippet = search.getSummary(document, queryTerms);
			String title = document.metadata.get("title");
			String url = document.metadata.get("url");

			if (snippet == null)
				snippet = "";

			response.setContentType("text/xml");
			writer.append("<response>\n");
			writer.append(String.format("<snippet>%s</snippet>\n", snippet));
			writer.append(String.format("<identifier>%s</identifier>\n",
					identifier));
			writer.append(String.format("<title>%s</title>\n", scrub(title)));
			writer.append(String.format("<url>%s</url>\n", scrub(url)));
			writer.append("</response>");
			writer.close();
		}
	}

	private String scrub(String s) throws UnsupportedEncodingException {
		if (s == null)
			return s;
		return s.replace("<", "&gt;").replace(">", "&lt;")
				.replace("&", "&amp;");
	}

	public void retrieveImage(OutputStream output) throws IOException {
		InputStream image = getClass().getResourceAsStream(
				"/images/images.jpeg");
		Utility.copyStream(image, output);
		output.close();
	}
	
	public void handleImage(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		OutputStream output = response.getOutputStream();
		response.setContentType("image/jpeg");
		retrieveImage(output);
		//OutputStream output1 = response.getOutputStream();
	
	}

	public void handleSearch(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		result = performSearch(request);
		response.setContentType("text/html");
		String displayQuery;
		String encodedQuery;
	
		 displayQuery = scrub(request.getParameter("q"));
		 check=displayQuery;
		 encodedQuery = URLEncoder.encode(request.getParameter("q"),
				"UTF-8");
		
		
		if (result.items.size() > 0) {
			PrintWriter writer = response.getWriter();
			String h = read_file();
			writer.append("<html>\n");
			writer.append("<head>\n");
			writer.append(String.format(
					"<title>%s - Spoken Query Retrieval</title>\n",
					displayQuery));
			writeStyle1(writer);
			writer.append("<script type=\"text/javascript\">\n");
			writer.append("function setvalue() {\n");
			writer.append("alert(\""+h+"\");");
			//writer.append("var a=document.getElementById(\"kunal\").value;");
			//writer.append("alert(\"hello\");\n");
			writer.append("document.getElementById(\"search2\").value=\""+h+"\";\n");
			writer.append("document.getElementById(\"myform1\").submit()");
			//writer.append("s.text(h)");
			//writer.append("  }\n");
			writer.append("}\n");
			writer.append("</script>\n");
			writer.append("<script type=\"text/javascript\">\n");
			
			writer.append("$(function(){");
			 
			    // add multiple select / deselect functionality
			    writer.append("$(\"#selectall\").click(function () {");
			          writer.append("$('.case').attr('checked', this.checked);");
			    writer.append("});");
			 
			    // if all checkbox are selected, check the selectall checkbox
			    // and viceversa
			    writer.append("$(\".case\").click(function(){");
			 
			    	writer.append("if($(\".case\").length == $(\".case:checked\").length) {");
			        	writer.append("$(\"#selectall\").attr(\"checked\", \"checked\");");
			            writer.append("} else {");
			        	writer.append("$(\"#selectall\").removeAttr(\"checked\");");
			            writer.append("}");
			 
			        writer.append (" });");
			writer.append("});");
		
			writer.append("</script>");
			writer.append("<script type=\"text/javascript\">\n");
			writer.append("function toggleDebug() {\n");
			writer.append("   var object = document.getElementById('debug');\n");
			writer.append("   if (object.style.display != 'block') {\n");
			writer.append("     object.style.display = 'block';\n");
			writer.append("  } else {\n");
			writer.append("     object.style.display = 'none';\n");
			writer.append("  }\n");
			writer.append("}\n");
			writer.append("</script>\n");
			writer.append("</head>\n<body>\n");

			writer.append("<div id=\"header\">\n");
			writer.append("<table><tr>");
			writer.append("<td>" + "<img src=\"/images/images.jpeg\"width=\"400\"></td>");
			// String h=server.speech;
			
			// writer.append("<td><br/><form action=\"search\">");
			// System.out.println("Server Speech "+h);
			// System.out.println("Server value "+server.i);
			// writer.append("<div id=\"Layer9\"><div id=\"Layer1\"><input id=\"gobutton\" type=\"submit\" value=\"Refresh for New Query!\"/></div><div id=\"Layer4\"><input type=\"radio\" id=\"radio1\" name=\"radios\" value=\"all\" checked>       <label for=\"radio1\">Average IDF</label>    <input type=\"radio\" id=\"radio2\" name=\"radios\"value=\"false\">       <label for=\"radio2\">Quality Scope</label></div><div id=\"Layer5\">  <label for=\"name\"><span class=\"style3\">Quality Predictors:</span></label></div><div class=\"style3\" id=\"Layer6\">Ranking Models:</div><div id=\"Layer7\"><input type=\"radio\" id=\"radio3\" name=\"radios2\" value=\"all\" checked>       <label for=\"radio3\">Dirichlet Model</label>    <input type=\"radio\" id=\"radio4\" name=\"radios2\"value=\"false\"><label for=\"radio4\">Vector-Space Model</label></div><form class=\"form-wrapper cf\">	<input type=\"text\" placeholder=\"Search here...\" required>	<button type=\"submit\">Search</button></form></div>");
			//writer.append("<form action=\"search\">");
			writer.append("<div id=\"Layer9\">" +
					//String.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><input id=\"gobutton\" type=\"submit\" value=\"Refresh for New Query!\"/></a>",h,0,10,App.s,App.s3) +
					"<form action=\"search\" id=\"myform1\">" +
					"<div id=\"Layer1\">" +
					String.format("<input type=\"text\" id=\"search2\" placeholder=\"Search here...\" value=\"%s\"size=\"30\" name=\"q\">	",displayQuery) +
							"<input id=\"gobutton\" type=\"submit\" value=\"Search\">" +
							"<input id=\"gobutton\" type=\"button\" value=\"Refresh Query\" onClick=\"setvalue()\"/>" +	
							"</div>" +
							"<div id=\"Layer4\">" +
							"<input type=\"radio\" id=\"radio1\" name=\"radios\" value=\"1\" checked>" +
							"<label for=\"radio1\">Average IDF</label>    " +
							"<input type=\"radio\" id=\"radio2\" name=\"radios\"value=\"2\">       " +
							"<label for=\"radio2\">Query Scope</label></div><div id=\"Layer5\">  " +
							"<label for=\"name\"><span class=\"style3\">Quality Predictors:</span></label>" +
							"</div>" +
							"<div class=\"style3\" id=\"Layer6\">Ranking Models:</div>" +
							"<div id=\"Layer7\">" +
							"<input type=\"radio\" id=\"radio3\" name=\"radios2\" value=\"1\" checked> " +
							"<label for=\"radio3\">Dirichlet Model</label>" +
							"<input type=\"radio\" id=\"radio4\" name=\"radios2\"value=\"2\">" +
							"<label for=\"radio4\">Vector-Space Model</label>" +
							"</div>" +
							"</form>" +
							"</div>");
			//writer.append("</form>");
			/*
			 * writer.append(String.format(
			 * "<input name=\"q\" size=\"40\" value=\"%s\"/>", displayQuery) +
			 * "<input value=\"Search\" type=\"submit\" /><br/><br/><br/>Quality Predictors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type=\"radio\" name=\"group1\" value=\"SCS\" checked=\"checked\" autocomplete=\"off\"/>SCS <input type=\"radio\" name=\"group1\" value=\"IDF\" autocomplete=\"off\"/>IDF <input type=\"radio\" name=\"group1\" value=\"Probability\" autocomplete=\"off\"/> Probability<br/><br/>Ranking Measure:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type=\"radio\" name=\"group2\" value=\"Dirichilet\" checked=\"checked\" autocomplete=\"off\"/>Dirichilet Ranking <input type=\"radio\" name=\"group2\" value=\"Vector\" autocomplete=\"off\"/>Vector-Based</form></td>"
			 * ); writer.append(String.format(
			 * "<td><a href=\"search?q=%s&start=%d&n=%d\">Refresh For New Spoken Query</a></td>"
			 * ,h, 0,10));
			 */
			writer.append("</tr>");
			writer.append("</table>\n");

			writer.append("</div>\n<br/><br/><br/><br/><br/><br/><br/>");

			writer.append("<center><font color=\"blue\"><h5>"
					+ String.format(
							"About %s result with in %s Miliseconds time",
							result.items.size(), (endTime - startTime) * -1)
					+ "</h5></font></center>");
			writer.append("<font color=#480000><h3>"
					+ String.format("Shorten Query-> %s ", Search.que)
					+ "</h3></font>");
			writer.append("<font color=#480000 ><h3>"
					+ String.format("Top Level Query-> %s ", Search.que1)
					+ "</h3></font>");
			writer.append("<center>[<a href=\"#\" onClick=\"toggleDebug(); return false;\"><font color=\"blue\">Expanded Query</font></a>]</center>");
			writer.append("<div id=\"debug\">");
			java.util.Iterator<String> it=Concat.iterator();
			   while(it.hasNext())
		    {   String kunal=it.next();
		    	writer.append(String.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><font color=\"blue\" size=\"5px\">%s</font></a>",
						kunal, 0, 10,SearchWebHandler.value,SearchWebHandler.value1,kunal));
		         writer.append("<br>");
		    }
		
		    
			/*writer.append("<table>");
			writer.append(String
					.format("<tr><td><font color=#480000 >%s</font></td><td><font color=#480000 >%s</font></td></tr>",
							"Parsed Query", result.query.toString()));
			writer.append(String
					.format("<tr><td><font color=#480000 >%s</font></td><td><font color=#480000 >%s</font></td></tr>",
							"Transformed Query",
							result.transformedQuery.toString()));
			writer.append("</table>");*/
			writer.append("</div>");
			
			writer.append("<center>");
			//int po=1;
			//writer.append("<input type=\"checkbox\"  name=\"case\" value=\"5\"/>Select");
			for (SearchResultItem item : result.items) {
				writer.append("<div id=\"result\" >\n");
				writer.append(String
						.format("<a href=\"document?identifier=%s\"><font color=\"blue\" size=\"5px\">%s</font></a><br/>"
								+ "<div id=\"summary\">%s</div>\n"
								+ "<div id=\"meta\">%s - %s</div>\n",
								item.identifier, item.displayTitle,
								item.summary, scrub(item.identifier),
								scrub(item.url)));
              //  writer.append("<input type=\"checkbox\" name=\"case\" value=\"5"+po+"\"/>Select");
				writer.append("</div>\n");
				//po++;
			}
	
			writer.append("</center>");
			// System.out.print(result.items.size());
			String startAtString = request.getParameter("start");
			String countString = request.getParameter("n");
			int startAt = 0;
			int count = 10;

			if (startAtString != null) {
				startAt = Integer.parseInt(startAtString);
			}
			if (countString != null) {
				count = Integer.parseInt(countString);
			}

			writer.append("<center>\n");
			if (startAt != 0) {
				writer.append(String
						.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><font color=#480000 >Previous</font></a>",
								encodedQuery, Math.max(startAt - count, 0),
								count,SearchWebHandler.value,SearchWebHandler.value1));
				if (result.items.size() >= count) {
					writer.append(" | ");
				}
			}

			if (result.items.size() >= count) {
				writer.append(String
						.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><font color=#480000 >Next</font></a>",
								encodedQuery, startAt + count, count,SearchWebHandler.value,SearchWebHandler.value1));
			}
			writer.append("</center>");
			writer.append("</body>");
			writer.append("</html>");
			writer.close();
		} else if (result.items.size() == 0) {
			String h = read_file();
			PrintWriter writer = response.getWriter();
			writer.append("<html>\n");
			writer.append("<head>\n");
			writer.append(String.format("<title>%s - Galago Search</title>\n",
					displayQuery));
			writeStyle1(writer);
			writer.append("<script type=\"text/javascript\">\n");
			writer.append("function setvalue() {\n");
			writer.append("alert(\""+h+"\");");
			//writer.append("var a=document.getElementById(\"kunal\").value;");
			//writer.append("alert(\"hello\");\n");
			writer.append("document.getElementById(\"search2\").value=\""+h+"\";\n");
			writer.append("document.getElementById(\"myform1\").submit()");
			//writer.append("s.text(h)");
			//writer.append("  }\n");
			writer.append("}\n");
			writer.append("</script>\n");
			writer.append("<script type=\"text/javascript\">\n");
			writer.append("function toggleDebug() {\n");
			writer.append("   var object = document.getElementById('debug');\n");
			writer.append("   if (object.style.display != 'block') {\n");
			writer.append("     object.style.display = 'block';\n");
			writer.append("  } else {\n");
			writer.append("     object.style.display = 'none';\n");
			writer.append("  }\n");
			writer.append("}\n");
			writer.append("</script>\n");
			writer.append("</head>\n<body>\n");

			writer.append("<div id=\"header\">\n");

			writer.append("<table><tr>");
			writer.append("<td>" + "<img src=\"/images/images.jpeg\" widht=10></td>");
			// String h=server.speech;
			// System.out.println("Server Speech "+h);
			// System.out.println("Server value "+server.i);
			
			//writer.append("<td><br/><form action=\"search\">");// +
			/*
			 * String.format("<input name=\"q\" size=\"40\" value=\"%s\"/>",
			 * displayQuery) +
			 * "<input value=\"Search\" type=\"submit\" /></form></td>");
			 * writer.append(String.format(
			 * "<td><a href=\"search?q=%s&start=%d&n=%d\">Refresh For New Spoken Query</a></td>"
			 * ,h, 0,10));
			 */
			writer.append("<div id=\"Layer9\">" +
					//String.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><input id=\"gobutton\" type=\"submit\" value=\"Refresh for New Query!\"/></a>",h,0,10,App.s,App.s3) +
					
					"<form action=\"search\" id=\"myform1\">" +
					"<div id=\"Layer1\">" +
					String.format("<input type=\"text\" id=\"search2\" placeholder=\"Search here...\" value=\"%s\"size=\"30\" name=\"q\">	",displayQuery) +
							"<input id=\"gobutton\" type=\"submit\" value=\"Search\">" +
							"<input id=\"gobutton\" type=\"button\" value=\"Refresh Query\" onClick=\"setvalue()\"/>" +		
							"</div>" +
							"<div id=\"Layer4\">" +
							"<input type=\"radio\" id=\"radio1\" name=\"radios\" value=\"1\" checked>" +
							"<label for=\"radio1\">Average IDF</label>    " +
							"<input type=\"radio\" id=\"radio2\" name=\"radios\"value=\"2\">       " +
							"<label for=\"radio2\">Query Scope</label></div><div id=\"Layer5\">  " +
							"<label for=\"name\"><span class=\"style3\">Quality Predictors:</span></label>" +
							"</div>" +
							"<div class=\"style3\" id=\"Layer6\">Ranking Models:</div>" +
							"<div id=\"Layer7\">" +
							"<input type=\"radio\" id=\"radio3\" name=\"radios2\" value=\"1\" checked> " +
							"<label for=\"radio3\">Dirichlet Model</label>" +
							"<input type=\"radio\" id=\"radio4\" name=\"radios2\"value=\"2\">" +
							"<label for=\"radio4\">Vector-Space Model</label>" +
							"</div>" +
							"</form>" +
							"</div>");
			//writer.append("<div id=\"Layer9\"><div id=\"Layer1\"><input type=\"text\" placeholder=\"Search here...\" size=\"50\">	<input id=\"gobutton\" type=\"submit\" value=\"Search\"><input id=\"gobutton\" type=\"submit\" value=\"Refresh for New Query!\"/></div><div id=\"Layer4\"><input type=\"radio\" id=\"radio1\" name=\"radios\" value=\"all\" checked>       <label for=\"radio1\">Average IDF</label>    <input type=\"radio\" id=\"radio2\" name=\"radios\"value=\"false\">       <label for=\"radio2\">Quality Scope</label></div><div id=\"Layer5\">  <label for=\"name\"><span class=\"style3\">Quality Predictors:</span></label></div><div class=\"style3\" id=\"Layer6\">Ranking Models:</div><div id=\"Layer7\"><input type=\"radio\" id=\"radio3\" name=\"radios2\" value=\"all\" checked>       <label for=\"radio3\">Dirichlet Model</label>    <input type=\"radio\" id=\"radio4\" name=\"radios2\"value=\"false\"><label for=\"radio4\">Vector-Space Model</label></div></div>");
			writer.append("</tr>");
			writer.append("</table>\n");
			writer.append("</div>\n<br/><br/><br/><br/><br/><br/><br/>");

			writer.append("<center><font color=\"blue\"><h5>"
					+ String.format(
							"About %s result with in %s Miliseconds time",
							result.items.size(), (endTime - startTime) * -1)
					+ "</h5></font></center></div>");
			// writer.append("<br/><br/><br/><br/><br/><center><font color=\"DimGray\"><h5>"+String.format("About %s result with in %s Miliseconds time",result.items.size(),(endTime-startTime)*-1)+"</h5></font></center>");
			// writer.append("<font color=#480000 ><h3>"+String.format("Shorten Query-> %s ",Search.que)+"</h3></font>");
			// writer.append("<font color=#480000 ><h3>"+String.format("Top Level Query-> %s ",Search.que1)+"</h3></font>");
			writer.append("<center>[<a href=\"#\" onClick=\"toggleDebug(); return false;\"><font color=\"blue\">Expanded Query</font></a>]</center>");
			writer.append("<div id=\"debug\">");
			java.util.Iterator<String> it=Concat.iterator();
			//System.out.println(Concat.size());
		    while(it.hasNext())
		    {   
		       String kunal=it.next();
		    	writer.append(String.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><font color=\"blue\" size=\"5px\">%s</font></a>",
					kunal, 0, 10,SearchWebHandler.value,SearchWebHandler.value1,kunal));
		         writer.append("<br>");
		    }
			
			
			/*writer.append("<table>");
			writer.append(String
					.format("<tr><td><font color=#480000 >%s</font></td><td><font color=#480000 >%s</font></td></tr>",
							"Parsed Query", result.query.toString()));
			writer.append(String
					.format("<tr><td><font color=#480000 >%s</font></td><td><font color=#480000 >%s</font></td></tr>",
							"Transformed Query",
							result.transformedQuery.toString()));
			writer.append("</table>");*/
			writer.append("</div>");

			writer.append("<h3><font color=#480000 size=\"5px\">Did You Mean? </font></h3>");
			// writer.append("<h3>But you can find your result by clicking one of them given below<h3>");
			 db();
			int i = 1;
			if (!displayQuery.equals("")) {
				displayQuery = capitalize(displayQuery);

				while(i!=10) {
					    Random r=new Random();
					    int j = r.nextInt(20);
					    String j1=val.get(j);
						String kl = displayQuery.concat(" ");
						kl = kl.concat(j1);
						writer.append(String.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><font color=\"blue\" size=\"5px\">%s%s%s</font></a>",
										kl, 0, 10,SearchWebHandler.value,SearchWebHandler.value1,displayQuery, " ", j1));
						writer.append("<br>");
						writer.append("<br>");
						i++;
					
				}

			} else {
				/*for (String j : kunal) {
					if (j != null && i != 11) {

						writer.append(String.format(
								"<a href=\"search?q=%s&start=%d&n=%d\">%s</a>",
								j, 0, 10, j));
						writer.append("<br>");
						writer.append("<br>");
						i++;
					}
				}*/
				 response.sendRedirect(App.location);
			}

			writer.append("</center>");
			writer.append("</body>");
			writer.append("</html>");
			writer.close();
		}
	}

	public void handleSearchXML(HttpServletRequest request,
			HttpServletResponse response) throws IllegalStateException,
			IllegalArgumentException, IOException, Exception {
		SearchResult result = performSearch(request);
		PrintWriter writer = response.getWriter();
		XMLOutputter outputter = new XMLOutputter(writer, "UTF-8");
		response.setContentType("text/xml");

		outputter.startTag("response");

		writer.append("<response>\n");
		// writer.append("<center>");
		for (SearchResultItem item : result.items) {
			outputter.startTag("result");

			outputter.startTag("identifier");
			outputter.pcdata(item.identifier);
			outputter.endTag();

			outputter.startTag("title");
			outputter.pcdata(item.displayTitle);
			outputter.endTag();

			outputter.startTag("url");
			outputter.pcdata(item.url);
			outputter.endTag();

			outputter.startTag("snippet");
			outputter.pcdata(item.summary);
			outputter.endTag();

			outputter.startTag("rank");
			outputter.pcdata("" + item.rank);
			outputter.endTag();

			outputter.startTag("metadata");
			for (Entry<String, String> entry : item.metadata.entrySet()) {
				outputter.startTag("item");
				outputter.startTag("key");
				outputter.pcdata(entry.getKey());
				outputter.endTag();
				outputter.startTag("value");
				outputter.pcdata(entry.getValue());
				outputter.endTag();
			}

			outputter.endTag();
		}
		// writer.append("</center>");
	}

	public void writeStyle(PrintWriter writer) {
		writer.write("<style type=\"text/css\">\n");
		writer.write("body { font-family: Helvetica, sans-serif; }\n");
		writer.write("img { border-style: none; }\n");
		writer.write("#box { border: 1px solid #ccc; margin: 100px auto; width: 500px;"
				+ "background: rgb(210, 233, 217); }\n");
		writer.write("#box a { font-size: small; text-decoration: none; }\n");
		writer.write("#box a:link { color: rgb(0, 93, 40); }\n");
		writer.write("#box a:visited { color: rgb(90, 93, 90); }\n");
		writer.write("#header { background: rgb(210, 233, 217); border: 1px solid #ccc; }\n");
		writer.write("#result { padding: 10px 5px; max-width: 550px; }\n");
		writer.write("#meta { font-size: small; color: rgb(60, 100, 60); }\n");
		writer.write("#summary { font-size: small; }\n");
		writer.write("#debug { display: none; }\n");

		writer.write("</style>");
	}

	public void writeStyle1(PrintWriter writer) {
		writer.write("<style type=\"text/css\">\n");
		// writer.write("body {background: #555 url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAB9JREFUeNpi/P//PwM6YGLAAuCCmpqacC2MRGsHCDAA+fIHfeQbO8kAAAAASUVORK5CYII=); font: 13px 'Lucida sans', Arial, Helvetica;        color: #eee;        text-align: center;    }       a {        color: #ccc;    }        /*-------------------------------------*/        .cf:before, .cf:after{      content:\"\";      display:table;    }        .cf:after{      clear:both;    }    .cf{      zoom:1;    }    /*-------------------------------------*/	        .form-wrapper {        width: 450px;        padding: 15px;        margin: 150px auto 50px auto;        background: #444;        background: rgba(0,0,0,.2);        -moz-border-radius: 10px;        -webkit-border-radius: 10px;        border-radius: 10px;        -moz-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);    }        .form-wrapper input {        width: 330px;        height: 20px;        padding: 10px 5px;        float: left;            font: bold 15px 'lucida sans', 'trebuchet MS', 'Tahoma';        border: 0;        background: #eee;        -moz-border-radius: 3px 0 0 3px;        -webkit-border-radius: 3px 0 0 3px;        border-radius: 3px 0 0 3px;          }	#footer{position:absolute;bottom:0;width:100%;height:30px;   /* Height of the footer */background:#6cf;clear: left;width: 100%;background: black;color: #FFF;text-align: center;}        .form-wrapper input:focus {        outline: 0;        background: #fff;        -moz-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        -webkit-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        box-shadow: 0 0 2px rgba(0,0,0,.8) inset;    }        .form-wrapper input::-webkit-input-placeholder {       color: #999;       font-weight: normal;       font-style: italic;    }        .form-wrapper input:-moz-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }        .form-wrapper input:-ms-input-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }            .form-wrapper button {		overflow: visible;        position: relative;        float: right;        border: 0;        padding: 0;        cursor: pointer;        height: 40px;        width: 110px;        font: bold 15px/40px 'lucida sans', 'trebuchet MS', 'Tahoma';        color: #fff;        text-transform: uppercase;        background: #d83c3c;        -moz-border-radius: 0 3px 3px 0;        -webkit-border-radius: 0 3px 3px 0;        border-radius: 0 3px 3px 0;              text-shadow: 0 -1px 0 rgba(0, 0 ,0, .3);    }             .form-wrapper button:hover{		        background: #e54040;    }	          .form-wrapper button:active,    .form-wrapper button:focus{           background: #c42f2f;        }        .form-wrapper button:before {        content: '';        position: absolute;        border-width: 8px 8px 8px 0;        border-style: solid solid solid none;        border-color: transparent #d83c3c transparent;        top: 12px;        left: -6px;    }        .form-wrapper button:hover:before{        border-right-color: #e54040;    }        .form-wrapper button:focus:before{        border-right-color: #c42f2f;    }            .form-wrapper button::-moz-focus-inner {        border: 0;        padding: 0;    }	input#gobutton{	cursor:pointer; /*forces the cursor to change to a hand when the button is hovered*/	padding:5px 25px; /*add some padding to the inside of the button*/background:#35b128; /*the colour of the button*/border:1px solid #33842a; /*required or the default border for the browser will appear*//*give the button curved corners, alter the size as required*/-moz-border-radius: 10px;-webkit-border-radius: 10px;border-radius: 10px;/*give the button a drop shadow*/-webkit-box-shadow: 0 0 4px rgba(0,0,0, .75);-moz-box-shadow: 0 0 4px rgba(0,0,0, .75);box-shadow: 0 0 4px rgba(0,0,0, .75);/*style the text*/color:#f3f3f3;font-size:1.1em;}/***NOW STYLE THE BUTTON'S HOVER AND FOCUS STATES***/input#gobutton:hover, input#gobutton:focus{background-color :#399630; /*make the background a little darker*//*reduce the drop shadow size to give a pushed button effect*/-webkit-box-shadow: 0 0 1px rgba(0,0,0, .75);-moz-box-shadow: 0 0 1px rgba(0,0,0, .75);box-shadow: 0 0 1px rgba(0,0,0, .75);}body { font-family: calibri;}input[type=radio], input[type=checkbox] {		display:none;	}input[type=radio] + label, input[type=checkbox] + label {		display:inline-block;		margin:-2px;		padding: 4px 12px;		margin-bottom: 0;		font-size: 14px;		line-height: 20px;		color: #333;		text-align: center;		text-shadow: 0 1px 1px rgba(255,255,255,0.75);		vertical-align: middle;		cursor: pointer;		background-color: #f5f5f5;		background-image: -moz-linear-gradient(top,#fff,#e6e6e6);		background-image: -webkit-gradient(linear,0 0,0 100%,from(#fff),to(#e6e6e6));		background-image: -webkit-linear-gradient(top,#fff,#e6e6e6);		background-image: -o-linear-gradient(top,#fff,#e6e6e6);		background-image: linear-gradient(to bottom,#fff,#e6e6e6);		background-repeat: repeat-x;		border: 1px solid #ccc;		border-color: #e6e6e6 #e6e6e6 #bfbfbf;		border-color: rgba(0,0,0,0.1) rgba(0,0,0,0.1) rgba(0,0,0,0.25);		border-bottom-color: #b3b3b3;		filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffffff',endColorstr='#ffe6e6e6',GradientType=0);		filter: progid:DXImageTransform.Microsoft.gradient(enabled=false);		-webkit-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);	}	 input[type=radio]:checked + label, input[type=checkbox]:checked + label{		   background-image: none;		outline: 0;		-webkit-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);			background-color:#e0e0e0;	}    #Layer1 {	position:absolute;	left:465px;	top:228px;	width:319px;	height:52px;	z-index:1;}    #Layer2 {	position:absolute;	left:443px;	top:302px;	width:463px;	height:48px;	z-index:2;}    #Layer3 {	position:absolute;	left:443px;	top:360px;	width:461px;	height:49px;	z-index:3;}    #Layer4 {	position:absolute;	left:684px;	top:308px;	width:208px;	height:32px;	z-index:4;}    #Layer5 {	position:absolute;	left:455px;	top:309px;	width:162px;	height:26px;	z-index:5;}    #Layer6 {	position:absolute;	left:459px;	top:369px;	width:144px;	height:24px;	z-index:6;}    #Layer7 {	position:absolute;	left:627px;	top:366px;	width:265px;	height:34px;	z-index:7;}.style3 {	font-size: 18px;	font-weight: bold;}    #Layer8 {	position:absolute;	left:402px;	top:68px;	width:478px;	height:65px;	z-index:8;}");
		writer.append("body {        font: 13px 'Lucida sans', Arial, Helvetica;        color: #eee;        text-align: center;    }        a {        color: #ccc;    }        /*-------------------------------------*/        .cf:before, .cf:after{      content:\"\";      display:table;    }        .cf:after{      clear:both;    }    .cf{      zoom:1;    }    /*-------------------------------------*/	        .form-wrapper {        width: 450px;        padding: 15px;        margin: 150px auto 50px auto;        background: #444;        background: rgba(0,0,0,.2);        -moz-border-radius: 10px;        -webkit-border-radius: 10px;        border-radius: 10px;        -moz-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);    }        .form-wrapper input {        width: 330px;        height: 20px;        padding: 10px 5px;        float: left;            font: bold 15px 'lucida sans', 'trebuchet MS', 'Tahoma';        border: 0;        background: #eee;        -moz-border-radius: 3px 0 0 3px;        -webkit-border-radius: 3px 0 0 3px;        border-radius: 3px 0 0 3px;          }	#footer{position:absolute;bottom:0;width:100%;height:30px;   /* Height of the footer */background:#6cf;clear: left;width: 100%;background: black;color: #FFF;text-align: center;}        .form-wrapper input:focus {        outline: 0;        background: #fff;        -moz-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        -webkit-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        box-shadow: 0 0 2px rgba(0,0,0,.8) inset;    }        .form-wrapper input::-webkit-input-placeholder {       color: #999;       font-weight: normal;       font-style: italic;    }        .form-wrapper input:-moz-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }        .form-wrapper input:-ms-input-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }            .form-wrapper button {		overflow: visible;        position: relative;        float: right;        border: 0;        padding: 0;        cursor: pointer;        height: 40px;        width: 110px;        font: bold 15px/40px 'lucida sans', 'trebuchet MS', 'Tahoma';        color: #fff;        text-transform: uppercase;        background: #d83c3c;        -moz-border-radius: 0 3px 3px 0;        -webkit-border-radius: 0 3px 3px 0;        border-radius: 0 3px 3px 0;              text-shadow: 0 -1px 0 rgba(0, 0 ,0, .3);    }             .form-wrapper button:hover{		        background: #e54040;    }	          .form-wrapper button:active,    .form-wrapper button:focus{           background: #c42f2f;        }        .form-wrapper button:before {        content: '';        position: absolute;        border-width: 8px 8px 8px 0;        border-style: solid solid solid none;        border-color: transparent #d83c3c transparent;        top: 12px;        left: -6px;    }        .form-wrapper button:hover:before{        border-right-color: #e54040;    }        .form-wrapper button:focus:before{        border-right-color: #c42f2f;    }            .form-wrapper button::-moz-focus-inner {        border: 0;        padding: 0;    }	input#gobutton{	cursor:pointer; /*forces the cursor to change to a hand when the button is hovered*/	padding:5px 25px; /*add some padding to the inside of the button*/background:#35b128; /*the colour of the button*/border:1px solid #33842a; /*required or the default border for the browser will appear*//*give the button curved corners, alter the size as required*/-moz-border-radius: 10px;-webkit-border-radius: 10px;border-radius: 10px;/*give the button a drop shadow*/-webkit-box-shadow: 0 0 4px rgba(0,0,0, .75);-moz-box-shadow: 0 0 4px rgba(0,0,0, .75);box-shadow: 0 0 4px rgba(0,0,0, .75);/*style the text*/color:#f3f3f3;font-size:1.1em;}/***NOW STYLE THE BUTTON'S HOVER AND FOCUS STATES***/input#gobutton:hover, input#gobutton:focus{background-color :#399630; /*make the background a little darker*//*reduce the drop shadow size to give a pushed button effect*/-webkit-box-shadow: 0 0 1px rgba(0,0,0, .75);-moz-box-shadow: 0 0 1px rgba(0,0,0, .75);box-shadow: 0 0 1px rgba(0,0,0, .75);}body { font-family: calibri;}input[type=radio], input[type=checkbox] {		display:none;	}input[type=radio] + label, input[type=checkbox] + label {		display:inline-block;		margin:-2px;		padding: 4px 12px;		margin-bottom: 0;		font-size: 14px;		line-height: 20px;		color: #333;		text-align: center;		text-shadow: 0 1px 1px rgba(255,255,255,0.75);		vertical-align: middle;		cursor: pointer;		background-color: #f5f5f5;		background-image: -moz-linear-gradient(top,#fff,#e6e6e6);		background-image: -webkit-gradient(linear,0 0,0 100%,from(#fff),to(#e6e6e6));		background-image: -webkit-linear-gradient(top,#fff,#e6e6e6);		background-image: -o-linear-gradient(top,#fff,#e6e6e6);		background-image: linear-gradient(to bottom,#fff,#e6e6e6);		background-repeat: repeat-x;		border: 1px solid #ccc;		border-color: #e6e6e6 #e6e6e6 #bfbfbf;		border-color: rgba(0,0,0,0.1) rgba(0,0,0,0.1) rgba(0,0,0,0.25);		border-bottom-color: #b3b3b3;		filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffffff',endColorstr='#ffe6e6e6',GradientType=0);		filter: progid:DXImageTransform.Microsoft.gradient(enabled=false);		-webkit-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);	}	 input[type=radio]:checked + label, input[type=checkbox]:checked + label{		   background-image: none;		outline: 0;		-webkit-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);			background-color:#e0e0e0;	}    #Layer1 {	position:absolute;	left:248px;	top:58px;	width:673px;	height:52px;	z-index:1;}    #Layer2 {	position:absolute;	left:443px;	top:302px;	width:463px;	height:48px;	z-index:2;}    #Layer3 {	position:absolute;	left:443px;	top:360px;	width:461px;	height:49px;	z-index:3;}    #Layer4 {	position:absolute;	left:594px;	top:121px;	width:300px;	height:32px;	z-index:4;}    #Layer5 {	position:absolute;	left:269px;	top:124px;	width:220px;	height:26px;	z-index:5;}    #Layer6 {	position:absolute;	left:273px;	top:180px;	width:200px;	height:24px;	z-index:6;}    #Layer7 {	position:absolute;	left:538px;	top:179px;	width:365px;	height:34px;	z-index:7;}.style3 {	font-size: 18px;	font-weight: bold; color:black;}    #Layer8 {	position:absolute;	left:15px;	top:17px;	width:478px;	height:65px;	z-index:8;}    #Layer9 {	position:absolute;	left:170px;	top:52px;	width:961px;	height:240px;	z-index:1;}    ");
		// writer.append("body {        background: #555 url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAB9JREFUeNpi/P//PwM6YGLAAuCCmpqacC2MRGsHCDAA+fIHfeQbO8kAAAAASUVORK5CYII=);        font: 13px 'Lucida sans', Arial, Helvetica;        color: #eee;        text-align: center;    }        a {        color: #ccc;    }        /*-------------------------------------*/        .cf:before, .cf:after{      content:\"\";      display:table;    }        .cf:after{      clear:both;    }    .cf{      zoom:1;    }    /*-------------------------------------*/	        .form-wrapper {        width: 450px;        padding: 15px;        margin: 150px auto 50px auto;        background: #444;        background: rgba(0,0,0,.2);        -moz-border-radius: 10px;        -webkit-border-radius: 10px;        border-radius: 10px;        -moz-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);    }        .form-wrapper input {        width: 330px;        height: 20px;        padding: 10px 5px;        float: left;            font: bold 15px 'lucida sans', 'trebuchet MS', 'Tahoma';        border: 0;        background: #eee;        -moz-border-radius: 3px 0 0 3px;        -webkit-border-radius: 3px 0 0 3px;        border-radius: 3px 0 0 3px;          }	#footer{position:absolute;bottom:0;width:100%;height:30px;   /* Height of the footer */background:#6cf;clear: left;width: 100%;background: black;color: #FFF;text-align: center;}        .form-wrapper input:focus {        outline: 0;        background: #fff;        -moz-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        -webkit-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        box-shadow: 0 0 2px rgba(0,0,0,.8) inset;    }        .form-wrapper input::-webkit-input-placeholder {       color: #999;       font-weight: normal;       font-style: italic;    }        .form-wrapper input:-moz-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }        .form-wrapper input:-ms-input-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }            .form-wrapper button {		overflow: visible;        position: relative;        float: right;        border: 0;        padding: 0;        cursor: pointer;        height: 40px;        width: 110px;        font: bold 15px/40px 'lucida sans', 'trebuchet MS', 'Tahoma';        color: #fff;        text-transform: uppercase;        background: #d83c3c;        -moz-border-radius: 0 3px 3px 0;        -webkit-border-radius: 0 3px 3px 0;        border-radius: 0 3px 3px 0;              text-shadow: 0 -1px 0 rgba(0, 0 ,0, .3);    }             .form-wrapper button:hover{		        background: #e54040;    }	          .form-wrapper button:active,    .form-wrapper button:focus{           background: #c42f2f;        }        .form-wrapper button:before {        content: '';        position: absolute;        border-width: 8px 8px 8px 0;        border-style: solid solid solid none;        border-color: transparent #d83c3c transparent;        top: 12px;        left: -6px;    }        .form-wrapper button:hover:before{        border-right-color: #e54040;    }        .form-wrapper button:focus:before{        border-right-color: #c42f2f;    }            .form-wrapper button::-moz-focus-inner {        border: 0;        padding: 0;    }	input#gobutton{	cursor:pointer; /*forces the cursor to change to a hand when the button is hovered*/	padding:5px 25px; /*add some padding to the inside of the button*/background:#35b128; /*the colour of the button*/border:1px solid #33842a; /*required or the default border for the browser will appear*//*give the button curved corners, alter the size as required*/-moz-border-radius: 10px;-webkit-border-radius: 10px;border-radius: 10px;/*give the button a drop shadow*/-webkit-box-shadow: 0 0 4px rgba(0,0,0, .75);-moz-box-shadow: 0 0 4px rgba(0,0,0, .75);box-shadow: 0 0 4px rgba(0,0,0, .75);/*style the text*/color:#f3f3f3;font-size:1.1em;}/***NOW STYLE THE BUTTON'S HOVER AND FOCUS STATES***/input#gobutton:hover, input#gobutton:focus{background-color :#399630; /*make the background a little darker*//*reduce the drop shadow size to give a pushed button effect*/-webkit-box-shadow: 0 0 1px rgba(0,0,0, .75);-moz-box-shadow: 0 0 1px rgba(0,0,0, .75);box-shadow: 0 0 1px rgba(0,0,0, .75);}body { font-family: calibri;}input[type=radio], input[type=checkbox] {		display:none;	}input[type=radio] + label, input[type=checkbox] + label {		display:inline-block;		margin:-2px;		padding: 4px 12px;		margin-bottom: 0;		font-size: 14px;		line-height: 20px;		color: #333;		text-align: center;		text-shadow: 0 1px 1px rgba(255,255,255,0.75);		vertical-align: middle;		cursor: pointer;		background-color: #f5f5f5;		background-image: -moz-linear-gradient(top,#fff,#e6e6e6);		background-image: -webkit-gradient(linear,0 0,0 100%,from(#fff),to(#e6e6e6));		background-image: -webkit-linear-gradient(top,#fff,#e6e6e6);		background-image: -o-linear-gradient(top,#fff,#e6e6e6);		background-image: linear-gradient(to bottom,#fff,#e6e6e6);		background-repeat: repeat-x;		border: 1px solid #ccc;		border-color: #e6e6e6 #e6e6e6 #bfbfbf;		border-color: rgba(0,0,0,0.1) rgba(0,0,0,0.1) rgba(0,0,0,0.25);		border-bottom-color: #b3b3b3;		filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffffff',endColorstr='#ffe6e6e6',GradientType=0);		filter: progid:DXImageTransform.Microsoft.gradient(enabled=false);		-webkit-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);	}	 input[type=radio]:checked + label, input[type=checkbox]:checked + label{		   background-image: none;		outline: 0;		-webkit-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);			background-color:#e0e0e0;	}    #Layer1 {	position:absolute;	left:248px;	top:5px;	width:673px;	height:52px;	z-index:1;}    #Layer2 {	position:absolute;	left:443px;	top:302px;	width:463px;	height:48px;	z-index:2;}    #Layer3 {	position:absolute;	left:443px;	top:360px;	width:461px;	height:49px;	z-index:3;}    #Layer4 {	position:absolute;	left:634px;	top:50px;	width:209px;	height:32px;	z-index:4;}    #Layer5 {	position:absolute;	left:310px;	top:50px;	width:162px;	height:26px;	z-index:5;}    #Layer6 {	position:absolute;	left:310px;	top:100px;	width:144px;	height:24px;	z-index:6;}    #Layer7 {	position:absolute;	left:580px;	top:100px;	width:265px;	height:34px;	z-index:7;}.style3 {	font-size: 18px;	font-weight: bold;}    #Layer8 {	position:absolute;	left:15px;	top:17px;	width:478px;	height:65px;	z-index:8;}    #Layer9 {	position:absolute;	left:170px;	top:52px;	width:961px;	height:200px;	z-index:1;}    ");
		// writer.write("body {       background: #555 url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAB9JREFUeNpi/P//PwM6YGLAAuCCmpqacC2MRGsHCDAA+fIHfeQbO8kAAAAASUVORK5CYII=);        font: 13px 'Lucida sans', Arial, Helvetica;        color: #eee;        text-align: center;    }        a {        color: #ccc;    }        /*-------------------------------------*/        .cf:before, .cf:after{      content:\"\";      display:table;    }        .cf:after{      clear:both;    }    .cf{      zoom:1;    }    /*-------------------------------------*/	        .form-wrapper {        width: 450px;        padding: 15px;        margin: 150px auto 50px auto;        background: #444;        background: rgba(0,0,0,.2);        -moz-border-radius: 10px;        -webkit-border-radius: 10px;        border-radius: 10px;        -moz-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);    }        .form-wrapper input {        width: 330px;        height: 20px;        padding: 10px 5px;        float: left;            font: bold 15px 'lucida sans', 'trebuchet MS', 'Tahoma';        border: 0;        background: #eee;        -moz-border-radius: 3px 0 0 3px;        -webkit-border-radius: 3px 0 0 3px;        border-radius: 3px 0 0 3px;          }	#footer{position:absolute;bottom:0;width:100%;height:30px;   /* Height of the footer */background:#6cf;clear: left;width: 100%;background: black;color: #FFF;text-align: center;}        .form-wrapper input:focus {        outline: 0;        background: #fff;        -moz-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        -webkit-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        box-shadow: 0 0 2px rgba(0,0,0,.8) inset;    }        .form-wrapper input::-webkit-input-placeholder {       color: #999;       font-weight: normal;       font-style: italic;    }        .form-wrapper input:-moz-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }        .form-wrapper input:-ms-input-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }            .form-wrapper button {		overflow: visible;        position: relative;        float: right;        border: 0;        padding: 0;        cursor: pointer;        height: 40px;        width: 110px;        font: bold 15px/40px 'lucida sans', 'trebuchet MS', 'Tahoma';        color: #fff;        text-transform: uppercase;        background: #d83c3c;        -moz-border-radius: 0 3px 3px 0;        -webkit-border-radius: 0 3px 3px 0;        border-radius: 0 3px 3px 0;              text-shadow: 0 -1px 0 rgba(0, 0 ,0, .3);    }             .form-wrapper button:hover{		        background: #e54040;    }	          .form-wrapper button:active,    .form-wrapper button:focus{           background: #c42f2f;        }        .form-wrapper button:before {        content: '';        position: absolute;        border-width: 8px 8px 8px 0;        border-style: solid solid solid none;        border-color: transparent #d83c3c transparent;        top: 12px;        left: -6px;    }        .form-wrapper button:hover:before{        border-right-color: #e54040;    }        .form-wrapper button:focus:before{        border-right-color: #c42f2f;    }            .form-wrapper button::-moz-focus-inner {        border: 0;        padding: 0;    }	input#gobutton{	cursor:pointer; /*forces the cursor to change to a hand when the button is hovered*/	padding:5px 25px; /*add some padding to the inside of the button*/background:#35b128; /*the colour of the button*/border:1px solid #33842a; /*required or the default border for the browser will appear*//*give the button curved corners, alter the size as required*/-moz-border-radius: 10px;-webkit-border-radius: 10px;border-radius: 10px;/*give the button a drop shadow*/-webkit-box-shadow: 0 0 4px rgba(0,0,0, .75);-moz-box-shadow: 0 0 4px rgba(0,0,0, .75);box-shadow: 0 0 4px rgba(0,0,0, .75);/*style the text*/color:#f3f3f3;font-size:1.1em;}/***NOW STYLE THE BUTTON'S HOVER AND FOCUS STATES***/input#gobutton:hover, input#gobutton:focus{background-color :#399630; /*make the background a little darker*//*reduce the drop shadow size to give a pushed button effect*/-webkit-box-shadow: 0 0 1px rgba(0,0,0, .75);-moz-box-shadow: 0 0 1px rgba(0,0,0, .75);box-shadow: 0 0 1px rgba(0,0,0, .75);}body { font-family: calibri;}input[type=radio], input[type=checkbox] {		display:none;	}input[type=radio] + label, input[type=checkbox] + label {		display:inline-block;		margin:-2px;		padding: 4px 12px;		margin-bottom: 0;		font-size: 14px;		line-height: 20px;		color: #333;		text-align: center;		text-shadow: 0 1px 1px rgba(255,255,255,0.75);		vertical-align: middle;		cursor: pointer;		background-color: #f5f5f5;		background-image: -moz-linear-gradient(top,#fff,#e6e6e6);		background-image: -webkit-gradient(linear,0 0,0 100%,from(#fff),to(#e6e6e6));		background-image: -webkit-linear-gradient(top,#fff,#e6e6e6);		background-image: -o-linear-gradient(top,#fff,#e6e6e6);		background-image: linear-gradient(to bottom,#fff,#e6e6e6);		background-repeat: repeat-x;		border: 1px solid #ccc;		border-color: #e6e6e6 #e6e6e6 #bfbfbf;		border-color: rgba(0,0,0,0.1) rgba(0,0,0,0.1) rgba(0,0,0,0.25);		border-bottom-color: #b3b3b3;		filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffffff',endColorstr='#ffe6e6e6',GradientType=0);		filter: progid:DXImageTransform.Microsoft.gradient(enabled=false);		-webkit-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);	}	 input[type=radio]:checked + label, input[type=checkbox]:checked + label{		   background-image: none;		outline: 0;		-webkit-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);			background-color:#e0e0e0;	}    #Layer1 {	position:absolute;	left:678px;	top:166px;	width:319px;	height:52px;	z-index:1;}    #Layer2 {	position:absolute;	left:443px;	top:302px;	width:463px;	height:48px;	z-index:2;}    #Layer3 {	position:absolute;	left:443px;	top:360px;	width:461px;	height:49px;	z-index:3;}    #Layer4 {	position:absolute;	left:509px;	top:236px;	width:209px;	height:32px;	z-index:4;}    #Layer5 {	position:absolute;	left:234px;	top:236px;	width:162px;	height:26px;	z-index:5;}    #Layer6 {	position:absolute;	left:235px;	top:283px;	width:144px;	height:24px;	z-index:6;}    #Layer7 {	position:absolute;	left:452px;	top:282px;	width:265px;	height:34px;	z-index:7;}.style3 {	font-size: 18px;	font-weight: bold;}#Layer9 {	position:absolute;	left:170px;	top:52px;	width:961px;	height:350px;	z-index:1;}");
		// writer.write("body { font-family: Helvetica, sans-serif; }\n");
		writer.write("img { border-style: none; }\n");
		writer.write("#box { border: 1px solid #ccc; margin: 100px auto; width: 500px;"
				+ "background: rgb(210, 233, 217); }\n");
		writer.write("#box a { font-size: medium; text-decoration: none; }\n");
		writer.write("#box a:link { color: rgb(0, 93, 40); }\n");
		writer.write("#box a:visited { color: rgb(90, 93, 90); }\n");
		// writer.write("#header { background: rgb(210, 233, 217); border: 1px solid #ccc; }\n");
		writer.write("#result { padding: 10px 5px; max-width: 650px; left:200px;}\n");
		writer.write("#meta { font-size: medium; color: rgb(60,100,60); }\n");
		writer.write("#summary { font-size: medium; color:black;}\n");
		writer.write("#debug { display: none;}\n");

		writer.write("</style>");
	}

	public void handleMainPage(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// String h=server.speech;
		// System.out.println("Server Speech "+h);
		// System.out.println("Server value "+server.i);
		String h = read_file();
		//System.out.println("Server Speech " + h);
		PrintWriter writer = response.getWriter();
		response.setContentType("text/html");

		writer.append("<html>\n");
		writer.append("<head>\n");
		// writeStyle(writer);
		writer.append("<script type=\"text/javascript\">\n");
		writer.append("function setvalue() {\n");
		writer.append("alert(\""+h+"\");");
		//writer.append("var a=document.getElementById(\"kunal\").value;");
		//writer.append("alert(\"hello\");\n");
		writer.append("document.getElementById(\"search1\").value=\""+h+"\";\n");
		writer.append("document.getElementById(\"myform\").submit()");
		//writer.append("s.text(h)");
		//writer.append("  }\n");
		writer.append("}\n");
		writer.append("</script>\n");
		writer.append("<title>Spoken Query Retrieval</title>");
		// writer.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"galagosearch-core/src/main/java/org/galagosearch/core/tools/spoken.css\">");
        
		writer.append("<style> body { background: #555 url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAB9JREFUeNpi/P//PwM6YGLAAuCCmpqacC2MRGsHCDAA+fIHfeQbO8kAAAAASUVORK5CYII=);        font: 13px 'Lucida sans', Arial, Helvetica;        color: #eee;        text-align: center;    }       a {        color: #ccc;    }                .cf:before, .cf:after{      content:\"\";      display:table;    }        .cf:after{      clear:both;    }    .cf{      zoom:1;    }            .form-wrapper {        width: 450px;        padding: 15px;        margin: 150px auto 50px auto;        background: #444;        background: rgba(0,0,0,.2);        -moz-border-radius: 10px;        -webkit-border-radius: 10px;        border-radius: 10px;        -moz-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);        box-shadow: 0 1px 1px rgba(0,0,0,.4) inset, 0 1px 0 rgba(255,255,255,.2);    }        .form-wrapper input {        width: 330px;        height: 30px;        padding: 20px 5px;        float: left;            font: bold 30px 'lucida sans', 'trebuchet MS', 'Tahoma';        border: 0;        background: #eee;        -moz-border-radius: 3px 0 0 3px;        -webkit-border-radius: 3px 0 0 3px;        border-radius: 3px 0 0 3px;          }	#footer{position:absolute;bottom:0;width:100%;height:30px;   background:#6cf;clear: left;width: 100%;background: black;color: #FFF;text-align: center;}        .form-wrapper input:focus {        outline: 0;        background: #fff;        -moz-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        -webkit-box-shadow: 0 0 2px rgba(0,0,0,.8) inset;        box-shadow: 0 0 2px rgba(0,0,0,.8) inset;    }        .form-wrapper input::-webkit-input-placeholder {       color: #999;       font-weight: normal;       font-style: italic;    }        .form-wrapper input:-moz-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }        .form-wrapper input:-ms-input-placeholder {        color: #999;        font-weight: normal;        font-style: italic;    }            .form-wrapper button {		overflow: visible;        position: relative;        float: right;        border: 0;        padding: 0;        cursor: pointer;        height: 40px;        width: 110px;        font: bold 15px/40px 'lucida sans', 'trebuchet MS', 'Tahoma';        color: #fff;        text-transform: uppercase;        background: #d83c3c;        -moz-border-radius: 0 3px 3px 0;        -webkit-border-radius: 0 3px 3px 0;        border-radius: 0 3px 3px 0;              text-shadow: 0 -1px 0 rgba(0, 0 ,0, .3);    }             .form-wrapper button:hover{		        background: #e54040;    }	          .form-wrapper button:active,    .form-wrapper button:focus{           background: #c42f2f;        }        .form-wrapper button:before {        content: '';        position: absolute;        border-width: 8px 8px 8px 0;        border-style: solid solid solid none;        border-color: transparent #d83c3c transparent;        top: 12px;        left: -6px;    }        .form-wrapper button:hover:before{        border-right-color: #e54040;    }        .form-wrapper button:focus:before{        border-right-color: #c42f2f;    }            .form-wrapper button::-moz-focus-inner {        border: 0;        padding: 0;    }	input#gobutton{	cursor:pointer; padding:5px 25px; background:#35b128; /*the colour of the button*/border:1px solid #33842a; /*required or the default border for the browser will appear*//*give the button curved corners, alter the size as required*/-moz-border-radius: 10px;-webkit-border-radius: 10px;border-radius: 10px;/*give the button a drop shadow*/-webkit-box-shadow: 0 0 4px rgba(0,0,0, .75);-moz-box-shadow: 0 0 4px rgba(0,0,0, .75);box-shadow: 0 0 4px rgba(0,0,0, .75);/*style the text*/color:#f3f3f3;font-size:1.1em;} input#gobutton1{	cursor:pointer; padding:0px 5px; background:#ff0000; /*the colour of the button*/border:1px solid #33842a; /*required or the default border for the browser will appear*//*give the button curved corners, alter the size as required*/-moz-border-radius: 10px;-webkit-border-radius: 10px;border-radius: 10px;/*give the button a drop shadow*/-webkit-box-shadow: 0 0 4px rgba(0,0,0, .75);-moz-box-shadow: 0 0 4px rgba(0,0,0, .75);box-shadow: 0 0 4px rgba(0,0,0, .75);/*style the text*/color:#f3f3f3;font-size:1.1em;}/***NOW STYLE THE BUTTON'S HOVER AND FOCUS STATES***/input#gobutton:hover, input#gobutton:focus{background-color :#399630; /*make the background a little darker*//*reduce the drop shadow size to give a pushed button effect*/-webkit-box-shadow: 0 0 1px rgba(0,0,0, .75);-moz-box-shadow: 0 0 1px rgba(0,0,0, .75);box-shadow: 0 0 1px rgba(0,0,0, .75);}body { font-family: calibri;}input[type=radio], input[type=checkbox] {		display:none;	}input[type=radio] + label, input[type=checkbox] + label {		display:inline-block;		margin:-2px;		padding: 4px 12px;		margin-bottom: 0;		font-size: 14px;		line-height: 20px;		color: #333;		text-align: center;		text-shadow: 0 1px 1px rgba(255,255,255,0.75);		vertical-align: middle;		cursor: pointer;		background-color: #f5f5f5;		background-image: -moz-linear-gradient(top,#fff,#e6e6e6);		background-image: -webkit-gradient(linear,0 0,0 100%,from(#fff),to(#e6e6e6));		background-image: -webkit-linear-gradient(top,#fff,#e6e6e6);		background-image: -o-linear-gradient(top,#fff,#e6e6e6);		background-image: linear-gradient(to bottom,#fff,#e6e6e6);		background-repeat: repeat-x;		border: 1px solid #ccc;		border-color: #e6e6e6 #e6e6e6 #bfbfbf;		border-color: rgba(0,0,0,0.1) rgba(0,0,0,0.1) rgba(0,0,0,0.25);		border-bottom-color: #b3b3b3;		filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffffff',endColorstr='#ffe6e6e6',GradientType=0);		filter: progid:DXImageTransform.Microsoft.gradient(enabled=false);		-webkit-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 1px 0 rgba(255,255,255,0.2),0 1px 2px rgba(0,0,0,0.05);	}	 input[type=radio]:checked + label, input[type=checkbox]:checked + label{		   background-image: none;		outline: 0;		-webkit-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		-moz-box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);		box-shadow: inset 0 2px 4px rgba(0,0,0,0.15),0 1px 2px rgba(0,0,0,0.05);			background-color:#e0e0e0;	}    #Layer1 {	position:absolute;	left:465px;	top:278px;	width:319px;	height:52px;	z-index:1;}    #Layer2 {	position:absolute;	left:380px;	top:393px;	width:600px;	height:48px;	z-index:2;}    #Layer3 {	position:absolute;	left:380px;	top:460px;	width:461px;	height:49px;	z-index:3;}    #Layer4 {	position:absolute;	left:700px;	top:400px;	width:320px;	height:32px;	z-index:4;}    #Layer5 {	position:absolute;	left:410px;	top:400px;	width:200px;	height:26px;	z-index:5;}    #Layer6 {	position:absolute;	left:400px;	top:469px;	width:200px;	height:24px;	z-index:6;}    #Layer7 {	position:absolute;	left:627px;	top:469px;	width:500px;	height:34px;	z-index:7;}.style3 {	font-size: 18px;	font-weight: bold;}    #Layer8 {	position:absolute;	left:402px;	top:68px;	width:478px;	height:65px;	z-index:8;}</style>");

		writer.append("</head>");
		writer.append("<body>");
		writer.append("<div id=\"Layer1\">");
		
		writer.append("<form action=\"search\" id=\"myform\">");
		// writer.append("<input type=\"text\" name=\"q\" placeholder=\"Search here...\" required>");
		writer.append("<input type=\"text\" id=\"search1\"name=\"q\" placeholder=\"Search here...\">");
		writer.append("<input id=\"gobutton1\"type=\"submit\" value=\"Search\"/>");
		writer.append("<input id=\"gobutton\" type=\"button\" value=\"Refresh Query\" onClick=\"setvalue()\"/>");
		// writer.append(String.format("<td><a href=\"search?q=%s&start=%d&n=%d\">Refresh For New Spoken Query</a></td>",h,
		// 0,10));
		//writer.append(String.format("<input id =\"kunal\" type=\"hidden\" value =%s />",h.toString()));
	    
		
		writer.append("</div>");
		writer.append("<div id=\"Layer2\">");
		writer.append("<label for=\"name\"></label>");
		writer.append("</div>");
		writer.append(" <div id=\"Layer3\">");

		writer.append("<label for=\"name\"></label>");
		writer.append("</div>");
		writer.append("<br>");
		writer.append("<br>");
		writer.append("<div id=\"Layer4\">");
		writer.append("<input type=\"radio\" id=\"radio1\" name=\"radios\" value=\"1\" checked>");
		writer.append("<label for=\"radio1\">Average IDF</label>");
		writer.append("<input type=\"radio\" id=\"radio2\" name=\"radios\" value=\"2\">");
		writer.append("<label for=\"radio2\">Query Scope</label>");
		writer.append("</div>");
		writer.append("<div id=\"Layer5\">");
		writer.append("<label for=\"name\"><span class=\"style3\">Quality Predictors:</span></label>");
		writer.append("</div>");
		writer.append("<div class=\"style3\" id=\"Layer6\">Ranking Models:</div>");
		writer.append("<div id=\"Layer7\">");
		writer.append("<input type=\"radio\" id=\"radio3\" name=\"radios2\" value=\"1\" checked>");
		writer.append("<label for=\"radio3\">Dirichlet Model</label>");
		writer.append("<input type=\"radio\" id=\"radio4\" name=\"radios2\"value=\"2\">");
		writer.append("<label for=\"radio4\">Vector-Space Model</label>");
		writer.append("</div>");
		writer.append("</form>");
		
		writer.append("<div id=\"Layer8\"><img src=\"images/images.jpeg\" alt=\"j\" width=\"479\" height=\"170\">");
		writer.append("<br>");
		//writer.append(String.format("<a href=\"search?q=%s&start=%d&n=%d&radios=%s&radios2=%s\"><input id=\"gobutton\" type=\"button\" value=\"Refresh Query\" onClick=\"setvalue();\"/></a>",h,0,10,App.s,App.s3));
		writer.append("</div>");
         
		writer.append("<div id=\"footer\">&copy;Jeevan Joishi and Kunal Gupta</a></div>");
		writer.append("</body>\n");
		writer.append("</html>\n");

		/*
		 * writer.append("<body>");
		 * 
		 * writer.append("<center><br/><br/><div>" + "<br/>\n");
		 * writer.append((String.format(
		 * "<form action=\"search\"><input name=\"q\" size=\"40\" value= %s>"+
		 * "<input value=\"Search\" type=\"submit\" /><br/><br/><br/>Quality Predictors: &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type=\"radio\" name=\"group1\" value=\"SCS\" checked=\"checked\" autocomplete=\"off\"/>SCS <input type=\"radio\" name=\"group1\" value=\"IDF\" autocomplete=\"off\"/>IDF <input type=\"radio\" name=\"group1\" value=\"Probability\" autocomplete=\"off\"/> Probability<br/><br/>Ranking Measure:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type=\"radio\" name=\"group2\" value=\"Dirichilet\" checked=\"checked\" autocomplete=\"off\"/>Dirichilet Ranking <input type=\"radio\" name=\"group2\" value=\"Vector\" autocomplete=\"off\"/>Vector-Based</form><br/><br/>"
		 * ,"Query---------------"))); writer.append(String.format(
		 * "<td><a href=\"search?q=%s&start=%d&n=%d\">Refresh For New Spoken Query</a></td>"
		 * ,h, 0,10)); writer.append("</div></center></body></html>\n");
		 */
		writer.close();

	}

	// <noscript><input type="submit" value="Submit"></noscript>
	//
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {
		startTime = System.currentTimeMillis();
		if (request.getPathInfo().equals("/search")) {
			try {
				handleSearch(request, response);
			} catch (Exception e) {
				throw new ServletException(
						"Caught exception from handleSearch", e);
			}
		} else if (request.getPathInfo().equals("/document")) {
			handleDocument(request, response);
		} else if (request.getPathInfo().equals("/searchxml")) {
			try {
				handleSearchXML(request, response);
			} catch (Exception e) {
				throw new ServletException(
						"Caught exception from handleSearchXML", e);
			}
		} else if (request.getPathInfo().equals("/snippet")) {
			handleSnippet(request, response);
		} else if (request.getPathInfo().startsWith("/images")) {
			handleImage(request, response);
		}

		else {
			handleMainPage(request, response);
		}
		endTime = System.currentTimeMillis();

	}

	private SearchResult performSearch(HttpServletRequest request)
			throws Exception {
		String query = request.getParameter("q");
		if(!check.equals(query))
		{
			po=1;
			precison.clear();
		}
		//System.out.println(query);
		value = request.getParameter("radios");
		//System.out.println(value);
		value1 = request.getParameter("radios2");
		//System.out.println(value1);
		Concat.clear();
		word(query);
		query = query.toLowerCase();
		
		String startAtString = request.getParameter("start");
		String countString = request.getParameter("n");
		int startAt = (startAtString == null) ? 0 : Integer
				.parseInt(startAtString);
		int resultCount = (countString == null) ? 10 : Integer
				.parseInt(countString);
		SearchResult result = search
				.runQuery(query, startAt, resultCount, true);
		return result;
		
		
	}

	public static String read_file() {
		FileInputStream fin = null;
		String h = "";
		try {
			  fin=new FileInputStream("/home/kunal/speech.txt");
			int content;
			while ((content = fin.read()) != -1) {
				Character c = (char) content;
				h = h.concat((c.toString()));

			}
			/*InputStreamReader isr = new InputStreamReader(ResouceLoader.load("speech.txt"));
			BufferedReader in = new BufferedReader(isr);
		    String str;
		    while ((str = in.readLine()) != null)
		    {
			     h=str;
		    }*/

		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {

				if (fin != null)
					fin.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return h;
	}

	// JDBC driver name and database URL

	public static void db() {
		int i=0;
		try
		{  
			//File file=new File("taggers/kunal.txt");
			//InputStream is=getResourceAsStream("3Columns.csv");
			InputStreamReader isr = new InputStreamReader(ResouceLoader.load("kunal.txt"));
			BufferedReader in = new BufferedReader(isr);
		    String str;
		    while ((str = in.readLine()) != null)
		    {
		    	val.put(i,str);
		    	i++;
		    }
		    in.close();
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
			
			
		/*String JDBC_DRIVER = "com.mysql.jdbc.Driver";
		String DB_URL = "jdbc:mysql://localhost/kunal";
		String[] kunal = new String[10];
		// Database credentials
		String USER = "username";
		String PASS = "password";
		Connection conn = null;
		Statement stmt = null;
		try {
			// STEP 2: Register JDBC driver
			Class.forName("com.mysql.jdbc.Driver");

			// STEP 3: Open a connection
			// System.out.println("Connecting to a selected database...");
			conn = DriverManager.getConnection(DB_URL, "root", "root");
			// System.out.println("Connected database successfully...");

			// STEP 4: Execute a query
			// System.out.println("Creating statement...");
			stmt = conn.createStatement();

			ResultSet rs = null;
			int i = 0;
			// for(int j=0;j<10;j++)
			// {

			String sql = "SELECT query FROM kunal ORDER BY RAND() Limit 10;";
			rs = stmt.executeQuery(sql);
			// STEP 5: Extract data from result set

			while (rs.next()) {
				// Retrieve by column name

				kunal[i] = rs.getString("query");
				i++;
				// Display values

			}
			// }
			rs.close();
		} catch (SQLException se) {
			// Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			// finally block used to close resources
			try {
				if (stmt != null)
					conn.close();
			} catch (SQLException se) {
			}// do nothing
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}// end finally try
		}// end try
		return kunal;*/
	}// end main

	public static String capitalize(String line) {
		return Character.toUpperCase(line.charAt(0)) + line.substring(1);
	}
	public static void word(String wordForm)
	{       String[] word=new String[5];
	        for(int i=0;i<5;i++)
	        {
	        	word[i]="";
	        }
	        int p=0;
		    String[] query1=wordForm.split(" ");
			WordNetDatabase database = WordNetDatabase.getFileInstance();
			for(int l=0;l<query1.length;l++)
			{
				/*Synset[] synsets = database.getSynsets(query1[l], SynsetType.NOUN);
				for (int i = 0; i < synsets.length; i++) {
				    nounSynset = (NounSynset)(synsets[i]);
				    hyponyms = nounSynset.getHyponyms();
				    System.err.println(nounSynset.getWordForms()[0]);
				} 
				*/
				
			Synset[] synsets = database.getSynsets(query1[l]);
			//  Display the word forms and definitions for synsets retrieved
			if (synsets.length > 0)
			{    
				 p=0;
				for (int i = 0; i < synsets.length; i++)
				{
					//System.out.println("Next");
					String wordForms="" ;
					String []wordForms1 = synsets[i].getWordForms();
					for(int j = 0; j < wordForms1.length; j++)
					{     //System.out.println(wordForms1[j]);
						if(wordForms1[j].equals(query1[l]))
						{  
							continue;
						}
						else
						{
							wordForms=wordForms1[j];
						}
					}
					
					word[p]=word[p].concat(wordForms+" ");
					p++;
					if(p==5)
					{
						break;
					}
					/*for(int j = 0; j < wordForms1.length; j++)
					{   
						   // word=word.concat(wordForms[j]+" ");
				 	    
						System.out.println((j > 0 ? ", " : "") +
								wordForms1[j]);
						
					}*/
					//System.out.println(": " + synsets[i].getDefinition());
				}
			}
			else
			{
				System.err.println("No Expansion or Similar Word exist that contain " +
						"the word form '" + query1[l] + "'");
				 for(int i=0;i<5;i++)
			        {
			        	word[i]=word[i].concat(query1[l]+" ");
			        }
				
			}
			//System.out.println("");
			//System.out.println("Next Phase of query word");
		}
			System.out.println("");
			
			for(int i=0;i<5;i++)
	        {  if(!Concat.contains(word[i]))
	            {
	        	Concat.add(word[i]);
	            }
	        else
	        {
	        	Concat.add(word[i]);
	        }
	        }
			
	}


}
