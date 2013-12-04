package fr.labri.gumtree.client;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class DiffOptions {

	@Option(name="-m", aliases={"--matcher"}, usage="The qualified name of the class implementing the matcher.")
	private String matcher = null;
	
	@Option(name="-g", aliases={"--generators"}, usage="A list of coma separated name of tree generators.")
	private String generators = null;

	@Option(name="-o", aliases={"--output"}, usage="web for the web-based client and swing for the swing-based client.")
	private String output = "web";
	
	@Argument(index=0,required=true)
	private String src;
	
	@Argument(index=1,required=true)
	private String dst;

	public String getMatcher() {
		return matcher;
	}

	public String getOutput() {
		return output;
	}

	public String getSrc() {
		return src;
	}
	
	public String getDst() {
		return dst;
	}
	
	public String[] getGenerators() {
		if (generators == null) return null;
		else return generators.split("\\.");
	}
	
}
