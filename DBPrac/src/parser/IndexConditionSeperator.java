
/**
 * Seperate the expression with column listed and the rest
 */
package parser;


import java.util.ArrayList;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

public class IndexConditionSeperator implements ExpressionVisitor {
	private String indexColumn;
	private Expression original;
	private int lowKey;
	private int highKey;
	private boolean flag;
	private String tableName;
	private String alias;
	

	R.A=R.B AND R.A=5 AND R.C=5
	
	public IndexConditionSeperator(String tableName, String alias,String column,Expression expr ) {
		original = expr;
		lowKey =Integer.MIN_VALUE;
		highKey= Integer.MAX_VALUE;
		this.indexColumn=column;
		original.accept(this);
		this.tableName= tableName;
		this.alias=alias;
	}
	
	public int getLowKey() {
		return lowKey;
	}
	
	public int getHighKey() {
		return highKey;
	}
	
	public Expression getRestExpr() {
		return original;
	}
	
	

	@Override
	public void visit(NullValue arg0) {
	}

	@Override
	public void visit(Function arg0) {
	}

	@Override
	public void visit(InverseExpression arg0) {
	}

	@Override
	public void visit(JdbcParameter arg0) {

	}

	@Override
	public void visit(DoubleValue arg0) {

	}

	@Override
	public void visit(LongValue arg0) {

	}

	@Override
	public void visit(DateValue arg0) {

	}

	@Override
	public void visit(TimeValue arg0) {
	}

	@Override
	public void visit(TimestampValue arg0) {
	}

	@Override
	public void visit(Parenthesis arg0) {

	}

	@Override
	public void visit(StringValue arg0) {

	}

	@Override
	public void visit(Addition arg0) {

	}

	@Override
	public void visit(Division arg0) {

	}

	@Override
	public void visit(Multiplication arg0) {

	}

	@Override
	public void visit(Subtraction arg0) {

	}

	@Override
	public void visit(AndExpression arg0) {
		flag = false;
		arg0.getLeftExpression().accept(this);
		if (flag) {
			arg0.setLeftExpression(null);
		}
		flag = false;
		arg0.getRightExpression().accept(this);
		if (flag) {
			arg0.setRightExpression(null);
		}
	}

	@Override
	public void visit(OrExpression arg0) {
		flag = false;
		arg0.getLeftExpression().accept(this);
		if (flag) {
			arg0.setLeftExpression(null);
		}
		flag = false;
		arg0.getRightExpression().accept(this);
		if (flag) {
			arg0.setRightExpression(null);
		}
	}

	@Override
	public void visit(Between arg0) {
	}

	@Override
	public void visit(EqualsTo arg0) {
		Expression left= arg0.getLeftExpression();
		Expression right= arg0.getRightExpression();
		if((left instanceof Column) && (right instanceof DoubleValue)) {
			boolean check = ((Column)left).getColumnName()==indexColumn 
					&& (((Column)left).getTable().getName()==tableName || ((Column)left).getTable().getName()==alias);
			if(check) {
				int value =(int) ((DoubleValue)right).getValue();
				lowKey = Math.max(lowKey,value);
				highKey = Math.min(highKey, value);
				flag = true;
			}
			
		}
		else if ((left instanceof DoubleValue)&& (right instanceof Column)) {
			if(((Column)right).getColumnName()==indexColumn &&  (((Column)right).getTable().getName()==tableName || ((Column)right).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)left).getValue();
				lowKey = Math.max(lowKey, value);
				highKey = Math.min(value, highKey);
				flag = true;
			}
		}
	}

	@Override
	public void visit(GreaterThan arg0) {
		Expression left= arg0.getLeftExpression();
		Expression right= arg0.getRightExpression();
		if((left instanceof Column) && (right instanceof DoubleValue)) {
			if(((Column)left).getColumnName()==indexColumn &&  (((Column)left).getTable().getName()==tableName || ((Column)left).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)right).getValue();
				lowKey = value+1;
				flag=true;
			}
			
		}
		else if ((left instanceof DoubleValue)&& (right instanceof Column)) {
			if(((Column)right).getColumnName()==indexColumn&&  (((Column)right).getTable().getName()==tableName || ((Column)right).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)left).getValue();
				highKey = value-1;
				flag=true;
			}
		}

	}

	@Override
	public void visit(GreaterThanEquals arg0) {
		Expression left= arg0.getLeftExpression();
		Expression right= arg0.getRightExpression();
		if((left instanceof Column) && (right instanceof DoubleValue)) {
			if(((Column)left).getColumnName()==indexColumn&&  (((Column)left).getTable().getName()==tableName || ((Column)left).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)right).getValue();
				lowKey = value;
				flag=true;
			}
			
		}
		else if ((left instanceof DoubleValue)&& (right instanceof Column)) {
			if(((Column)right).getColumnName()==indexColumn&&  (((Column)right).getTable().getName()==tableName || ((Column)right).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)left).getValue();
				highKey = value;
				flag=true;
			}
		}
	}

	@Override
	public void visit(InExpression arg0) {

	}

	@Override
	public void visit(IsNullExpression arg0) {

	}

	@Override
	public void visit(LikeExpression arg0) {
	}

	@Override
	public void visit(MinorThan arg0) {
		Expression left= arg0.getLeftExpression();
		Expression right= arg0.getRightExpression();
		if((left instanceof Column) && (right instanceof DoubleValue)) {
			if(((Column)left).getColumnName()==indexColumn &&(((Column)left).getTable().getName()==tableName || ((Column)left).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)right).getValue();
				highKey = value-1;
				flag=true;

			}
			
		}
		else if ((left instanceof DoubleValue)&& (right instanceof Column)) {
			if(((Column)right).getColumnName()==indexColumn &&  (((Column)right).getTable().getName()==tableName || ((Column)right).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)left).getValue();
				lowKey = value+1;
				flag=true;

			}
		}
		

	}

	@Override
	public void visit(MinorThanEquals arg0) {
		Expression left= arg0.getLeftExpression();
		Expression right= arg0.getRightExpression();
		if((left instanceof Column) && (right instanceof DoubleValue)) {
			if(((Column)left).getColumnName()==indexColumn &&(((Column)left).getTable().getName()==tableName || ((Column)left).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)right).getValue();
				highKey = value;
				flag=true;

			}
			
		}
		else if ((left instanceof DoubleValue)&& (right instanceof Column)) {
			if(((Column)right).getColumnName()==indexColumn&&  (((Column)right).getTable().getName()==tableName || ((Column)right).getTable().getName()==alias)) {
				int value =(int) ((DoubleValue)left).getValue();
				lowKey = value;
				flag=true;

			}
		}

	}

	@Override
	public void visit(NotEqualsTo arg0) {
	}

	@Override
	public void visit(Column arg0) {
	}

	@Override
	public void visit(SubSelect arg0) {

	}

	@Override
	public void visit(CaseExpression arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(WhenClause arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(ExistsExpression arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AllComparisonExpression arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(AnyComparisonExpression arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Concat arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Matches arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(BitwiseAnd arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(BitwiseOr arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(BitwiseXor arg0) {
		// TODO Auto-generated method stub

	}

}
