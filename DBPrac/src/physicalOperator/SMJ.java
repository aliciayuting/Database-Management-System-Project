package physicalOperator;

import java.util.ArrayList;
import java.util.HashMap;

import dataStructure.Tuple;
import fileIO.TupleWriter;
import net.sf.jsqlparser.expression.Expression;
import parser.EvaluateJoin;

public class SMJ extends Operator {
	private Operator leftOp;
	private Operator rightOp;
	private ArrayList<String> leftColList;
	private ArrayList<String> rightColList;
	private ExternalSortOperator leftExSortOp;
	private ExternalSortOperator rightExSortOp;
	private Tuple tr;
	private Tuple ts;
	private Tuple gs;
	private int ptr;
	boolean flag;

	private boolean ensureEqual(Tuple leftTup, Tuple rightTup, ArrayList<String> leftColList,
		ArrayList<String> rightColList, ArrayList<String> leftSchema, ArrayList<String> rightSchema, int k) {
		for (int i= 0; i < k; i+= 1) {
			System.out.println("righ  col is " + rightColList.toString());
			System.out.println(rightTup);

			System.out.println("right  schema is   " + rightSchema.toString());
			if (leftTup.getData(leftSchema.indexOf(leftColList.get(i))) != rightTup
				.getData(rightSchema.indexOf(rightColList.get(i)))) { return false; }
		}
		return true;
	}

	public SMJ(int bufferSize, Operator left, Operator right, Expression joinExpr, HashMap<String, String> alias,
		String dir) {
		EvaluateJoin evalJoin= new EvaluateJoin(joinExpr, left.getTableName(), right.getTableName(), alias);
		leftColList= evalJoin.getJoinAttributesLeft();
		rightColList= evalJoin.getJoinAttributesRight();
		leftOp= left;
		rightOp= right;
		leftExSortOp= new ExternalSortOperator(leftOp, leftColList, bufferSize, dir, "left");
		rightExSortOp= new ExternalSortOperator(rightOp, rightColList, bufferSize, dir, "right");
		tr= leftExSortOp.getNextTuple();
		Tuple firstTuple= rightExSortOp.getNextTuple();
		ts= firstTuple;
		gs= firstTuple;
		ptr= 0;
		flag= false;
	}

	@Override
	public Tuple getNextTuple() {
		while (tr != null && gs != null) {
			if (!flag) {
				int i= 0;
				while (i < leftColList.size()) {
					while (tr != null && gs != null && tr.getData(leftOp.schema().indexOf(leftColList.get(i))) < gs
						.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
						System.out.println("this tr is : " + tr.printData());
						tr= leftExSortOp.getNextTuple();
						System.out.println(" yeeeesssssssssss  tr is : " + tr.printData());
						if (!ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema(), i)) {
							i= 0;
							break;
						}
						System.out.println(" nnnnnnnnnow  tr is : " + tr.printData());
					}
					while (tr != null && gs != null && tr.getData(leftOp.schema().indexOf(leftColList.get(i))) > gs
						.getData(rightOp.schema().indexOf(rightColList.get(i)))) {
						System.out.println("     ------that is   tr is : " + tr.printData());
						ArrayList<Integer> a= new ArrayList<Integer>();
						a.add(67);
						a.add(91);
						a.add(10);
						if (tr.getTuple().equals(a)) {
							System.out.println("");
						}
						gs= rightExSortOp.getNextTuple();
						System.out.println("     read right------that is   tr is : " + tr.printData());
						ptr+= 1;
						if (!ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema(), i)) {
							i= -1;
							break;
						}
					}
					i+= 1;
				}
				ts= new Tuple(gs.getTuple());
			}
			if (tr == null || gs == null || tr.getTuple().size() == 0 || gs.getTuple().size() == 0) return null;
			if (ensureEqual(tr, gs, leftColList, rightColList, leftOp.schema(), rightOp.schema(),
				leftColList.size())) {
				if (ts != null && ts.getTuple().size() > 0 &&
					ensureEqual(tr, ts, leftColList, rightColList, leftOp.schema(), rightOp.schema(),
						leftColList.size())) {
					flag= true;
					Tuple joinedTuple= new Tuple();
					for (int j= 0; j < leftOp.schema().size(); j++ ) {
						joinedTuple.addData(tr.getData(j));
					}
					for (int j= 0; j < rightOp.schema().size(); j++ ) {
						joinedTuple.addData(ts.getData(j));
					}
					ts= rightExSortOp.getNextTuple();
					ptr+= 1;
					return joinedTuple;
				} else {
					flag= false;
					tr= leftExSortOp.getNextTuple();
					rightExSortOp.resetIndex(ptr);
				}
			}
		}
		return null;
	}

	@Override
	public void dump(TupleWriter writer) {
		Tuple t;
		while ((t= getNextTuple()) != null) {
			writer.addNextTuple(t);
		}
		writer.dump();
		writer.close();
	}

	@Override
	public void reset() {
		leftExSortOp.reset();
		rightExSortOp.reset();
		tr= leftExSortOp.getNextTuple();
		Tuple firstTuple= rightExSortOp.getNextTuple();
		ts= firstTuple;
		gs= firstTuple;
	}

	@Override
	public ArrayList<String> schema() {
		leftOp.schema().addAll(rightOp.schema());
		return leftOp.schema();
	}

	@Override
	public String getTableName() {
		return leftOp.getTableName() + "," + rightOp.getTableName();
	}
}
