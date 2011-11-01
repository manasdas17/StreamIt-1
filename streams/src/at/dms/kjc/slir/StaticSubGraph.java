package at.dms.kjc.slir;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.dms.kjc.backendSupport.CompCommRatio;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.MultiLevelSplitsJoins;
import at.dms.kjc.flatgraph.DataFlowTraversal;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.sir.SIRFileWriter;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIROperator;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.lowering.SIRScheduler;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;

/**
 * 
 * 
 * @author mgordon and soule
 * 
 */
public class StaticSubGraph {

	/** Identity filters generated by the conversion to SLIR */
	private HashSet<WorkNode> generatedIds;

	/** The input port for this static subgraph */
	private InputPort inputPort;

	/** Any filters that have a FileReader or FileWriter */
	public Filter[] io;

	/** The output port of this static subgraph */
	private OutputPort outputPort;

	/** The StreamGraph that contains this subgraph */
	private StreamGraph parent;

	/** Mapping from each SIRFilter to it's content */
	protected HashMap<SIRFilter, WorkNodeContent> sirToContent;

	/** The roots of the separate trees in the forest */
	protected LinkedList<Filter> topFilters;

	/** a work estimate for a Filter */
	private WorkEstimate work = null;

	/** filter content -> work estimation */
	protected HashMap<WorkNodeContent, Long> workEstimation;

	/**
	 * Create a StaticSubGraph.
	 * 
	 */
	public StaticSubGraph() {

	}

	/**
	 * Add the slice to the list of top slices, roots of the forest.
	 */
	public void addTopSlice(Filter slice) {
		topFilters.add(slice);
	}

	/**
	 * Does the the slice graph contain slice (perform a simple linear search).
	 * 
	 * @param filter
	 *            The filter to query.
	 * 
	 * @return True if the static sub graph contains filter.
	 */
	public boolean containsFilter(Filter filter) {
		Filter[] filterGraph = getFilterGraph();
		for (int i = 0; i < filterGraph.length; i++)
			if (filterGraph[i] == filter)
				return true;
		return false;
	}

	/**
	 * Force creation of kopi methods and fields for predefined filters.
	 */
	public void createPredefinedContent() {
		for (Filter s : getFilterGraph()) {
			if (s.getWorkNode().getFilter() instanceof PredefinedContent) {
				((PredefinedContent) s.getWorkNode().getFilter())
						.createContent();
			}
		}

	}


	
	/**
	 * Dump the the completed partition to a dot file
	 */
	public void dumpGraph(String filename) {
		Filter[] sliceGraph = getFilterGraph();
		StringBuffer buf = new StringBuffer();
		buf.append("digraph Flattend {\n");
		buf.append("size = \"8, 10.5\";\n");

		for (int i = 0; i < sliceGraph.length; i++) {
			Filter slice = sliceGraph[i];
			assert slice != null;
			buf.append(slice.hashCode() + " [ " + filterName(slice) + "\" ];\n");
			Filter[] next = getNext(slice/* ,parent */);
			for (int j = 0; j < next.length; j++) {
				assert next[j] != null;
				buf.append(slice.hashCode() + " -> " + next[j].hashCode()
						+ ";\n");
			}
		}

		buf.append("}\n");
		// write the file
		try {
			FileWriter fw = new FileWriter(filename);
			fw.write(buf.toString());
			fw.close();
		} catch (Exception e) {
			System.err.println("Could not print extracted slices");
		}
	}

	/**
	 * 
	 * @param filename
	 * @param layout
	 */
	public void dumpGraph(String filename,
			@SuppressWarnings("rawtypes") Layout layout) {
		dumpGraph(filename, layout, true);
	}

	/**
	 * Dump the the completed partition to a dot file
	 * 
	 * @param filename
	 * @param layout
	 * @param fullInfo
	 */
	@SuppressWarnings("rawtypes")
	public void dumpGraph(String filename, Layout layout, boolean fullInfo) {
		Filter[] sliceGraph = getFilterGraph();
		StringBuffer buf = new StringBuffer();
		buf.append("digraph Flattend {\n");
		buf.append("size = \"8, 10.5\";\n");

		for (int i = 0; i < sliceGraph.length; i++) {
			Filter slice = sliceGraph[i];
			assert slice != null;
			buf.append(slice.hashCode() + " [ "
					+ sliceName(slice, layout, fullInfo) + "\" ];\n");
			Filter[] next = getNext(slice/* ,parent */, SchedulingPhase.STEADY);
			for (int j = 0; j < next.length; j++) {
				assert next[j] != null;
				buf.append(slice.hashCode() + " -> " + next[j].hashCode()
						+ ";\n");
			}
			next = getNext(slice, SchedulingPhase.INIT);
			for (int j = 0; j < next.length; j++) {
				assert next[j] != null;
				buf.append(slice.hashCode() + " -> " + next[j].hashCode()
						+ "[style=dashed,color=red];\n");
			}
		}

		buf.append("}\n");
		// write the file
		try {
			FileWriter fw = new FileWriter(filename);
			fw.write(buf.toString());
			fw.close();
		} catch (Exception e) {
			System.err.println("Could not print extracted slices");
		}
	}

	/**
	 * Return a string with all of the names of the filter nodes and blue if
	 * linear
	 * 
	 * @param filter
	 * @return
	 */
	protected String filterName(Filter filter) {
		InternalFilterNode node = filter.getInputNode();

		StringBuffer out = new StringBuffer();

		// do something fancy for linear slices!!!
		if (((WorkNode) node.getNext()).getFilter().getArray() != null)
			out.append("color=cornflowerblue, style=filled, ");

		out.append("label=\"" + node.getAsInput().debugString(true));// toString());

		node = node.getNext();
		while (node != null) {
			if (node.isWorkNode()) {
				WorkNodeContent f = node.getAsFilter().getFilter();
				out.append("\\n" + node.toString() + "{" + getWorkEstimate(f)
						+ "}");
				if (f.isTwoStage())
					out.append("\\npre:(peek, pop, push): ("
							+ f.getPreworkPeek() + ", " + f.getPreworkPop()
							+ "," + f.getPreworkPush());
				out.append(")\\n(peek, pop, push: (" + f.getPeekInt() + ", "
						+ f.getPopInt() + ", " + f.getPushInt() + ")");
				out.append("\\nMult: init " + f.getInitMult() + ", steady "
						+ f.getSteadyMult());
				out.append("\\n *** ");
			} else {
				out.append("\\n" + node.getAsOutput().debugString(true));
			}
			/*
			 * else { //out.append("\\n" + node.toString()); }
			 */
			node = node.getNext();
		}
		return out.toString();
	}

	/**
	 * This used to be the flattenInternal in FlattenAndPartition.
	 * 
	 * @param filterNodes
	 * @param top
	 * @param edges
	 * @param bottleNeckFilter
	 * @param sliceBNWork
	 * @param workEstimation
	 * @param work
	 * @param topFilters
	 */
	private void flatten(SIRToFilterNodes filterNodes, FlatNode top,
			Map<OutputNode, HashMap<InputNode, InterFilterEdge>> edges,
			Map<WorkNodeContent, Long> workEstimation, WorkEstimate work,
			LinkedList<Filter> topFilters) {

		Filter topFilter = null;
		List<Filter> filterList = new LinkedList<Filter>();
		List<Filter> ioList = new LinkedList<Filter>();

		Iterator<FlatNode> dataFlow = DataFlowTraversal.getTraversal(top)
				.iterator();

		while (dataFlow.hasNext()) {
			FlatNode node = dataFlow.next();

			System.out
					.println("StaticSubGraph.flatten, examining Flatnode node="
							+ node.getName());
			System.out
					.println("StaticSubGraph.flatten, examining Flatnode node="
							+ node.getName() + " inputs.length=" + node.inputs
							+ " outputs.length=" + node.ways);
	
			InputNode input = filterNodes.inputNodes.get(node.contents);
			OutputNode output = filterNodes.outputNodes.get(node.contents);
			WorkNode filterNode = filterNodes.filterNodes.get(node.contents);

			assert input != null && output != null && filterNode != null;

			// set up the slice
			Filter filter = new Filter();
			input.setNext(filterNode);
			filterNode.setPrevious(input);
			filterNode.setNext(output);
			output.setPrevious(filterNode);
			input.setParent(filter);
			output.setParent(filter);
			filterNode.setParent(filter);
			filter.setInputNode(input);
			filter.setOutputNode(output);
			filter.setWorkNode(filterNode);

			if (node.ways != 0) {
				assert node.ways == node.getEdges().length
						&& node.ways == node.weights.length;

				// set up the i/o arcs
				// set up the splitting...
				LinkedList<InterFilterEdge> outEdges = new LinkedList<InterFilterEdge>();
				LinkedList<Integer> outWeights = new LinkedList<Integer>();
				HashMap<InputNode, InterFilterEdge> newEdges = new HashMap<InputNode, InterFilterEdge>();
				for (int i = 0; i < node.ways; i++) {
					if (node.weights[i] == 0)
						continue;
					InterFilterEdge edge = new InterFilterEdge(
							output,
							filterNodes.inputNodes.get(node.getEdges()[i].contents));
					newEdges.put(
							filterNodes.inputNodes
									.get(node.getEdges()[i].contents),
							edge);
					outEdges.add(edge);
					outWeights.add(node.weights[i]);
				}

				assert newEdges != null : "newEdges is null";
				assert output != null : "output is null";
				assert edges != null : "edges is null";

				edges.put(output, newEdges);

				LinkedList<LinkedList<InterFilterEdge>> translatedEdges = new LinkedList<LinkedList<InterFilterEdge>>();
				if (node.isDuplicateSplitter()) {
					outWeights = new LinkedList<Integer>();
					outWeights.add(new Integer(1));
					translatedEdges.add(outEdges);
				} else {
					for (int i = 0; i < outEdges.size(); i++) {
						LinkedList<InterFilterEdge> link = new LinkedList<InterFilterEdge>();
						link.add(outEdges.get(i));
						translatedEdges.add(link);
					}
				}

				output.set(outWeights, translatedEdges, SchedulingPhase.STEADY);
			} else {
				// no outputs
				output.setWeights(new int[0]);
				output.setDests(new InterFilterEdge[0][0]);
			}

			if (node.isFilter()) {
				if (node.getFilter().getPushInt() == 0) {
					output.setWeights(new int[0]);
					output.setDests(new InterFilterEdge[0][0]);
				}
			}

			if (node.inputs != 0) {
				assert node.inputs == node.incoming.length
						&& node.inputs == node.incomingWeights.length;

				LinkedList<Integer> inWeights = new LinkedList<Integer>();
				LinkedList<InterFilterEdge> inEdges = new LinkedList<InterFilterEdge>();
				for (int i = 0; i < node.inputs; i++) {
					if (node.incomingWeights[i] == 0)
						continue;

					assert edges != null : "Line 398; edges==null";
					assert filterNodes.outputNodes != null : "Line 398; outputNodes==null";
					assert node.incoming[i].contents != null : "Line 398; node.incoming[i].contents==null";

					inEdges.add(edges.get(
							filterNodes.outputNodes
									.get(node.incoming[i].contents)).get(input));
					inWeights.add(node.incomingWeights[i]);
				}
				input.set(inWeights, inEdges, SchedulingPhase.STEADY);
			} else {
				input.setWeights(new int[0]);
				input.setSources(new InterFilterEdge[0]);
			}

			if (node.isFilter() && node.getFilter().getPopInt() == 0) {
				input.setWeights(new int[0]);
				input.setSources(new InterFilterEdge[0]);
			}

			// set up the work hashmaps
			long workEst = 0;
			if (filterNodes.generatedIds.contains(filterNode)) {
				workEst = 3 * filterNode.getFilter().getSteadyMult();
			} else {
				assert node.isFilter();
				workEst = work.getWork((SIRFilter) node.contents);
			}

			workEstimation.put(filterNode.getFilter(), workEst);

			filter.finish();

			if (node.contents instanceof SIRFileReader
					|| node.contents instanceof SIRFileWriter) {
				// System.out.println("Found io " + node.contents);
				ioList.add(filter);
			}

			if (topFilter == null)
				topFilter = filter;
			filterList.add(filter);
		}
		// topSlices = new LinkedList<Filter>();
		topFilters.add(topFilter);
		
		
		System.out.println("StaticSubGraph.flatten start printing top filters");
		for (Filter f : topFilters) {
			System.out.println("StaticSubGraph.flatten " + f);
		}
		System.out.println("StaticSubGraph.flatten start printing top filters");
		
		System.out.println(topFilters);

		io = ioList.toArray(new Filter[ioList.size()]);
	}

	/**
	 * 
	 * @param f
	 * @return
	 */
	public WorkNodeContent getContent(SIRFilter f) {
		return sirToContent.get(f);
	}

	/**
	 * Get all slices
	 * 
	 * @return All the slices of the slice graph.
	 */
	public Filter[] getFilterGraph() {
		// new slices may have been added so we need to reconstruct the graph
		// each time
		LinkedList<Filter> filterGraph = DataFlowOrder.getTraversal(topFilters
				.toArray(new Filter[topFilters.size()]));

		return filterGraph.toArray(new Filter[filterGraph.size()]);
	}

	/**
	 * @param node
	 *            The Filter
	 * @return The work estimation for the filter slice node for one
	 *         steady-state mult of the filter.
	 */
	public long getFilterWork(WorkNode node) {
		return workEstimation.get(node.getFilter()).longValue();
	}

	/**
	 * @param node
	 * @return The work estimation for the filter for one steady-state
	 *         multiplied by the steady-state multiplier
	 */
	public long getFilterWorkSteadyMult(WorkNode node) {
		return getFilterWork(node) * parent.getSteadyMult();
	}

	/**
	 * 
	 * @return
	 */
	public HashSet<WorkNode> getGeneratedIds() {
		return generatedIds;
	}

	/**
	 * @return The InputPort for the StaticSubGraph
	 */
	public InputPort getInputPort() {
		return inputPort;
	}

	/**
	 * Get the downstream slices we cannot use the edge[] of slice because it is
	 * for execution order and this is not determined yet.
	 * 
	 * @param slice
	 * @return
	 */
	protected Filter[] getNext(Filter slice) {
		InternalFilterNode node = slice.getInputNode();
		if (node instanceof InputNode)
			node = node.getNext();
		while (node != null && node instanceof WorkNode) {
			node = node.getNext();
		}
		if (node instanceof OutputNode) {
			InterFilterEdge[][] dests = ((OutputNode) node)
					.getDests(SchedulingPhase.STEADY);
			ArrayList<Object> output = new ArrayList<Object>();
			for (int i = 0; i < dests.length; i++) {
				InterFilterEdge[] inner = dests[i];
				for (int j = 0; j < inner.length; j++) {
					// Object next=parent.get(inner[j]);
					Object next = inner[j].getDest().getParent();
					if (!output.contains(next))
						output.add(next);
				}
			}
			Filter[] out = new Filter[output.size()];
			output.toArray(out);
			return out;
		}
		return new Filter[0];
	}

	/**
	 * Get the downstream slices we cannot use the edge[] of slice because it is
	 * for execution order and this is not determined yet.
	 * 
	 * @param slice
	 * @param phase
	 * @return
	 */
	protected Filter[] getNext(Filter slice, SchedulingPhase phase) {
		InternalFilterNode node = slice.getInputNode();
		if (node instanceof InputNode)
			node = node.getNext();
		while (node != null && node instanceof WorkNode) {
			node = node.getNext();
		}
		if (node instanceof OutputNode) {
			InterFilterEdge[][] dests = ((OutputNode) node).getDests(phase);
			ArrayList<Object> output = new ArrayList<Object>();
			for (int i = 0; i < dests.length; i++) {
				InterFilterEdge[] inner = dests[i];
				for (int j = 0; j < inner.length; j++) {
					// Object next=parent.get(inner[j]);
					Object next = inner[j].getDest().getParent();
					if (!output.contains(next))
						output.add(next);
				}
			}
			Filter[] out = new Filter[output.size()];
			output.toArray(out);
			return out;
		}
		return new Filter[0];
	}

	/**
	 * @return The OutputPort for the StaticSubGraph
	 */
	public OutputPort getOutputPort() {
		return outputPort;
	}

	public StreamGraph getParent() {
		return parent;
	}

	/**
	 * Get just top level filters in the filter graph.
	 * 
	 * @return top level filters
	 */
	public Filter[] getTopFilters() {
		assert topFilters != null;
		return topFilters.toArray(new Filter[topFilters.size()]);
	}

	/**
	 * 
	 * @param fc
	 * @return
	 */
	public long getWorkEstimate(WorkNodeContent fc) {
		assert workEstimation.containsKey(fc);
		return workEstimation.get(fc).longValue();
	}

	/**
	 * The cost of 1 firing of the filter, to be run after the steady multiplier
	 * has been accounted for in the steady multiplicity of each filter content.
	 * 
	 * @param node
	 * @return
	 */
	public long getWorkEstOneFiring(WorkNode node) {
		return (getFilterWork(node) / (node.getFilter().getSteadyMult() / parent
				.getSteadyMult()));
	}

	/**
	 * @return
	 */
	public boolean hasDynamicInput() {
		if (inputPort == null) {
			return false;
		}
		return true;
	}

	/**
	 * @return
	 */
	public boolean hasDynamicOutput() {
		if (outputPort == null) {
			return false;
		}
		return true;
	}

	/**
	 * This function MUST be called after the constructor. I don't want to do
	 * this work in the constructor, in case something fails.
	 * 
	 * @param parent
	 *            The StreamGraph that contains this subgraph
	 * @param str
	 *            The SIR graph used to generate this subgraph
	 * @param inputPort
	 *            The input port
	 * @param outputPort
	 *            The output port
	 * @return this
	 */
	public StaticSubGraph init(StreamGraph parent, SIRStream str,
			InputPort inputPort, OutputPort outputPort) {

		this.parent = parent;
		this.inputPort = inputPort;
		this.outputPort = outputPort;

		workEstimation = new HashMap<WorkNodeContent, Long>();
		topFilters = new LinkedList<Filter>();
		sirToContent = new HashMap<SIRFilter, WorkNodeContent>();
		Map<OutputNode, HashMap<InputNode, InterFilterEdge>> edges = new HashMap<OutputNode, HashMap<InputNode, InterFilterEdge>>();

		
		if (str instanceof SIRFilter) {
			System.out.println("StaticSubGraph.init() str=" + str.getName() + " isStateful=" + ((SIRFilter)str).isStateful());
		}
		
		GraphFlattener fg = new GraphFlattener(str);
		SIRToFilterNodes filterNodes = new SIRToFilterNodes();
		Map<SIROperator, int[]>[] executionCounts = SIRScheduler
				.getExecutionCounts(str);
		
		System.out.println("StaticSubGraph.init() ");
		filterNodes.createNodes(fg.top, executionCounts);

		work = WorkEstimate.getWorkEstimate(str);

		// Print out computation to communication ratio.
		double CCRatio = CompCommRatio.ratio(str, work, executionCounts[1]);
		System.out.println("Comp/Comm Ratio of SIR graph: " + CCRatio);

		flatten(filterNodes, fg.top, edges, workEstimation, work, topFilters);

		this.setGeneratedIds(filterNodes.generatedIds);
		return this;
	}

	/**
	 * Check for I/O in slice
	 * 
	 * @param slice
	 * @return Return true if this slice is an IO slice (file reader/writer).
	 */
	public boolean isIO(Filter slice) {
		for (int i = 0; i < io.length; i++) {
			if (slice == io[i])
				return true;
		}
		return false;
	}

	/**
	 * Return true if the slice is a top (source) slice in the forrest
	 */
	public boolean isTopSlice(Filter slice) {
		for (Filter cur : topFilters) {
			if (cur == slice)
				return true;
		}
		return false;
	}

	/**
	 * Remove this slice from the list of top slices, roots of the forest.
	 * 
	 * @param slice
	 */
	public void removeTopSlice(Filter slice) {
		assert topFilters.contains(slice);
		topFilters.remove(slice);
	}

	/**
	 * 
	 * @param generatedIds
	 */
	public void setGeneratedIds(HashSet<WorkNode> generatedIds) {
		this.generatedIds = generatedIds;
	}

	/**
	 * 
	 * @param inputPort
	 */
	public void setInputPort(InputPort inputPort) {
		this.inputPort = inputPort;
	}

	/**
	 * 
	 * @param outputPort
	 */
	public void setOutputPort(OutputPort outputPort) {
		this.outputPort = outputPort;
	}

	public void setParent(StreamGraph parent) {
		this.parent = parent;
	}


	/**
	 * Set the slice graph to slices, where the only difference between the
	 * previous slice graph and the new slice graph is the addition of identity
	 * slices (meaning slices with only an identities filter).
	 * 
	 * @param slices
	 *            The new slice graph.
	 */
	public void setSliceGraphNewIds(Filter[] slices) {
		// add the new filters to the necessary structures...
		for (int i = 0; i < slices.length; i++) {
			if (!containsFilter(slices[i])) {
				WorkNode filter = slices[i].getWorkNode();
				assert filter.toString().startsWith("Identity");

				if (!workEstimation.containsKey(filter)) {
					// for a work estimation of an identity filter
					// multiple the estimated cost of on item by the number
					// of items that passes through it (determined by the
					// schedule mult).
					workEstimation
							.put(filter.getFilter(),
									(long) (MultiLevelSplitsJoins.IDENTITY_WORK * filter
											.getFilter().getSteadyMult()));
				}
			}
		}
	}

	/**
	 * Return a string with all of the names of the filter nodes and blue if
	 * linear
	 * 
	 * @param slice
	 * @param layout
	 * @param fullInfo
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected String sliceName(Filter slice, Layout layout, boolean fullInfo) {
		InternalFilterNode node = slice.getInputNode();

		StringBuffer out = new StringBuffer();

		// do something fancy for linear slices!!!
		if (((WorkNode) node.getNext()).getFilter().getArray() != null)
			out.append("color=cornflowerblue, style=filled, ");

		out.append("label=\"" + slice.hashCode() + "\\n");
		if (fullInfo)
			out.append(node.getAsInput()
					.debugString(true, SchedulingPhase.INIT)
					+ "\\n"
					+ node.getAsInput().debugString(true,
							SchedulingPhase.STEADY));// toString());

		node = node.getNext();
		while (node != null) {
			if (node.isWorkNode()) {
				WorkNodeContent f = node.getAsFilter().getFilter();
				out.append("\\n" + node.toString() + "{" + "}");
				if (f.isTwoStage())
					out.append("\\npre:(peek, pop, push): ("
							+ f.getPreworkPeek() + ", " + f.getPreworkPop()
							+ "," + f.getPreworkPush());
				out.append(")\\n(peek, pop, push: (" + f.getPeekInt() + ", "
						+ f.getPopInt() + ", " + f.getPushInt() + ")");
				out.append("\\nMult: init " + f.getInitMult() + ", steady "
						+ f.getSteadyMult());
				if (layout != null)
					out.append("\\nTile: "
							+ layout.getComputeNode(slice.getWorkNode())
									.getUniqueId());
				out.append("\\n *** ");
			} else {
				if (fullInfo)
					out.append("\\n"
							+ node.getAsOutput().debugString(true,
									SchedulingPhase.INIT)
							+ "\\n"
							+ node.getAsOutput().debugString(true,
									SchedulingPhase.STEADY));

			}
			/*
			 * else { //out.append("\\n" + node.toString()); }
			 */
			node = node.getNext();
		}
		return out.toString();
	}


	/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

	/** Returns a deep clone of this object. */
	public Object deepClone() {
		at.dms.kjc.slir.StaticSubGraph other = new at.dms.kjc.slir.StaticSubGraph();
		at.dms.kjc.AutoCloner.register(this, other);
		deepCloneInto(other);
		return other;
	}
	
	/**
	 * Clones all fields of this into
	 * 
	 * <pre>
	 * other
	 * </pre>
	 */
	protected void deepCloneInto(at.dms.kjc.slir.StaticSubGraph other) {
		other.generatedIds = (java.util.HashSet) at.dms.kjc.AutoCloner
				.cloneToplevel(this.generatedIds);
		other.inputPort = (at.dms.kjc.slir.InputPort) at.dms.kjc.AutoCloner
				.cloneToplevel(this.inputPort);
		other.io = (at.dms.kjc.slir.Filter[]) at.dms.kjc.AutoCloner
				.cloneToplevel(this.io);
		other.outputPort = (at.dms.kjc.slir.OutputPort) at.dms.kjc.AutoCloner
				.cloneToplevel(this.outputPort);
		other.parent = (at.dms.kjc.slir.StreamGraph) at.dms.kjc.AutoCloner
				.cloneToplevel(this.parent);
		other.sirToContent = (java.util.HashMap) at.dms.kjc.AutoCloner
				.cloneToplevel(this.sirToContent);
		other.topFilters = (java.util.LinkedList) at.dms.kjc.AutoCloner
				.cloneToplevel(this.topFilters);
		other.work = (at.dms.kjc.sir.lowering.partition.WorkEstimate) at.dms.kjc.AutoCloner
				.cloneToplevel(this.work);
		other.workEstimation = (java.util.HashMap) at.dms.kjc.AutoCloner
				.cloneToplevel(this.workEstimation);
	}

	
	/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
