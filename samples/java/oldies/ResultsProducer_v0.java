package fr.labri.vpraxis.refact.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fr.labri.vpraxis.actions.Action;
import fr.labri.vpraxis.actions.io.IActionReader;
import fr.labri.vpraxis.actions.io.xml.XMLActionReader;
import fr.labri.vpraxis.actions.utils.Pair;
import fr.labri.vpraxis.refact.StatesMatcher;

public class ResultsProducer {

	public static void main(String[] args) {
		ResultsProducer r = new ResultsProducer(args[0]);
		r.run();
	}

	private String rootPath;

	private FileWriter CSVWriter;

	private FileWriter HTMLWriter;

	private Map<Long,Set<Pair<String,String>>> expValsMap;
	
	private int tfound = 0;
	
	private int texact = 0;
	
	private int texpert = 0;
	
	private static DecimalFormat nb = new DecimalFormat("#0.00");

	public ResultsProducer(String rootPath) {
		this.rootPath = rootPath;
	}

	public void run() {
		try {
			loadExpValsMap();

			CSVWriter = new FileWriter(rootPath + ".res.csv");
			HTMLWriter = new FileWriter(rootPath + ".res.html");
			appendHTMLHeader();

			for(long rev : new TreeSet<Long>(expValsMap.keySet()))
				handleRevision(rev);

			double tprec = (double) texact / (double) tfound;
			double trec = (double) texact / (double) texpert;
			
			HTMLWriter.append("<h1>Global indicators</h1>\n");
			HTMLWriter.append("<p><b>Found : " + tfound + ", expert : " + texpert + ", exact: " + texact + "</b></p>\n");
			HTMLWriter.append("<p><b>Precision : " + nb.format(tprec) + ", Rappel : " + nb.format(trec) + "</b></p>\n");
			
			HTMLWriter.append("</div>\n</body>\n</html>\n");

			HTMLWriter.close();
			CSVWriter.close();
		} 

		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleRevision(long rev) throws IOException {
		IActionReader rprevs = new XMLActionReader(rootPath + "_" + (rev-1) + ".xml");
		IActionReader rcurs = new XMLActionReader(rootPath + "_" + (rev) + ".xml");

		List<Action> prevs = rprevs.read();
		List<Action> curs = rcurs.read();

		HTMLWriter.append("<h1>Mappings between rev. " + (rev-1) + " and rev. " + rev + "</h1>\n");
		HTMLWriter.append("<table>\n");

		long tic = System.currentTimeMillis();
		StatesMatcher matcher = new StatesMatcher(prevs, curs , 0.5D, 0.95D, 0.75D, new double[] {0.15,0.65,0.20}, 10);
		Set<Pair<String,String>> mappings = matcher.computeMappings();
		long toc = System.currentTimeMillis();

		int found = mappings.size();
		tfound += found;
		int expert = expValsMap.get(rev).size();
		texpert += expert;

		int exact = 0;
		
		Set<Pair<String,String>> scorrect = new TreeSet<Pair<String,String>>(new StringPairComparator());
		Set<Pair<String,String>> sincorrect = new TreeSet<Pair<String,String>>(new StringPairComparator());
		Set<Pair<String,String>> smissing = new TreeSet<Pair<String,String>>(new StringPairComparator());

		for(Pair<String,String> mapping : mappings) {
			if ( expValsMap.get(rev).contains(mapping) ) {
				exact++;
				scorrect.add(mapping);
			}
			else
				sincorrect.add(mapping);
		}
		
		texact += exact;
		
		for(Pair<String,String> mapping : expValsMap.get(rev))
			if ( !scorrect.contains(mapping) )
				smissing.add(mapping);
		
		double prec = (double) exact / (double) found;
		double rec = (double) exact / (double) expert;
		
		HTMLWriter.append("<p><b>Found : " + found + ", expert : " + expert + ", exact: " + exact + "</b></p>\n");
		HTMLWriter.append("<p><b>Precision : " + nb.format(prec) + ", rappel : " + nb.format(rec) + "</b></p>\n");
		
		appendRows(scorrect,"correct");
		appendRows(sincorrect,"incorrect");
		appendRows(smissing,"missing");

		HTMLWriter.append("</table>\n");

		CSVWriter.append(rev + ";" + prec + ";" + rec + ";" + (toc-tic) + "\n");
	}
	
	private void appendRows(Set<Pair<String,String>> mappings,String cls) throws IOException {
		for( Pair<String,String> mapping : mappings )
			HTMLWriter.append("<tr><td class=\"" + cls + "\">" + enc(mapping.getFirst()) + "</td><td class=\"" + cls + "\">" + enc(mapping.getSecond()) + "</td></tr>\n");
	}
	
	private void appendHTMLHeader() throws IOException {
		HTMLWriter.append("<html>\n");
		HTMLWriter.append("<head>\n");
		HTMLWriter.append("<title>" + rootPath + "</title>\n");
		HTMLWriter.append("<style type=\"text/css\">\n");
		appendCSS();
		HTMLWriter.append("</style>\n");
		HTMLWriter.append("</head>\n");
		HTMLWriter.append("<body>\n");
		HTMLWriter.append("<div class=\"container\">\n");
	}
	
	private void appendCSS() throws IOException {
		HTMLWriter.append("body {	margin:0;	padding:0;	color: black;	background-color: #CECECE;	font-family: Calibri, Helvetica, DejaVuSansCondensed, sans-serif;	font-size:100%;	width: 100%;	text-align: center;}p {	display: inline;	margin: 0;	padding: 0.25em; font-size: 85%; }h1 {	padding: 0;	margin-bottom: 1em;	font-size:125%;	font-weight:bold;	text-align: left;	color: #A50E37;	border-bottom: 1px solid #7BA05B;}div {	margin:0;	padding:0;}table {	border-collapse: collapse;	font-size : 75%;	width: 100%;	margin: 0;	padding: 0;}.container {	margin: 1em auto; 	padding: 1em;	width:90%;	background-color: white;	text-align: left;	-webkit-border-radius: 1em;	-moz-border-radius: 1em;}th {	margin:0;	padding:0;	font-weight : bold;	font-size : 100%;}tr {	margin:0;	padding:0;}td {	margin:0;	padding:0;	width: 50%;	border:1px solid black;}.correct {	background-color: rgb(96,175,92);}.incorrect {	background-color: rgb(228,149,31);}.missing {	background-color: white;}");
		HTMLWriter.append("\n");
	}

	private void loadExpValsMap() throws FileNotFoundException, IOException {
		expValsMap = new HashMap<Long, Set<Pair<String,String>>>();
		BufferedReader r = new BufferedReader(new FileReader(new File(rootPath + ".csv")));
		while ( r.ready() ) {
			String l = r.readLine();
			String[] tokens = l.split("\\;");
			long rev = Long.parseLong(tokens[0].trim());
			String src = tokens[1].trim();
			String tgt = tokens[2].trim();

			if ( !expValsMap.containsKey(rev) )
				expValsMap.put(rev,new HashSet<Pair<String,String>>());

			expValsMap.get(rev).add(new Pair<String,String>(src,tgt));
		}
		r.close();
	}
	
	private static String enc(String s) {
		return s.replaceAll("\\<","&lt;").replaceAll("\\>","&gt;");
	}
	
}
