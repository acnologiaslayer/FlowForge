package com.flowforge.model.task;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.TaskExecutionException;
import com.flowforge.model.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies an arithmetic operation to two operands and stores the result in
 * a named variable. Operands may be literal numbers or {@code ${variable}}
 * references resolved at run time, so a compute step can build on earlier
 * results.
 */
public class ComputeTask extends Task {

    /** The supported arithmetic operators. */
    public enum Operator {
        ADD("+"), SUBTRACT("-"), MULTIPLY("*"), DIVIDE("/");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Operator fromSymbol(String symbol) {
            for (Operator op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: " + symbol);
        }
    }

    private final String resultVariable;
    private final String leftOperand;
    private final Operator operator;
    private final String rightOperand;

    public ComputeTask(String name, String resultVariable, String leftOperand,
                       Operator operator, String rightOperand)
            throws InvalidTaskConfigurationException {
        super(name);
        if (resultVariable == null || resultVariable.isBlank()) {
            throw new InvalidTaskConfigurationException("Result variable must not be blank.");
        }
        if (operator == null) {
            throw new InvalidTaskConfigurationException("Operator must not be null.");
        }
        if (leftOperand == null || leftOperand.isBlank()
                || rightOperand == null || rightOperand.isBlank()) {
            throw new InvalidTaskConfigurationException("Both operands are required.");
        }
        this.resultVariable = resultVariable.trim();
        this.leftOperand = leftOperand.trim();
        this.operator = operator;
        this.rightOperand = rightOperand.trim();
    }

    public String getResultVariable() {
        return resultVariable;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getRightOperand() {
        return rightOperand;
    }

    @Override
    public TaskType getType() {
        return TaskType.COMPUTE;
    }

    @Override
    protected String execute(ExecutionContext context) throws TaskExecutionException {
        double left = resolveOperand(context, leftOperand);
        double right = resolveOperand(context, rightOperand);

        double result = switch (operator) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> {
                if (right == 0) {
                    throw new TaskExecutionException(getName(), "division by zero.");
                }
                yield left / right;
            }
        };

        String formatted = formatNumber(result);
        context.put(resultVariable, formatted);
        return String.format("%s = %s %s %s = %s",
                resultVariable, formatNumber(left), operator.getSymbol(),
                formatNumber(right), formatted);
    }

    private double resolveOperand(ExecutionContext context, String operand)
            throws TaskExecutionException {
        String resolved = context.interpolate(operand);
        try {
            return Double.parseDouble(resolved.trim());
        } catch (NumberFormatException e) {
            throw new TaskExecutionException(getName(),
                    "operand '" + operand + "' resolved to '" + resolved
                            + "', which is not a number.");
        }
    }

    /** Prints whole results without a trailing ".0" for readability. */
    private static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    @Override
    public String summary() {
        return "Compute: " + resultVariable + " = " + leftOperand + " "
                + operator.getSymbol() + " " + rightOperand;
    }

    @Override
    public Map<String, String> toFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("resultVariable", resultVariable);
        fields.put("leftOperand", leftOperand);
        fields.put("operator", operator.getSymbol());
        fields.put("rightOperand", rightOperand);
        return fields;
    }
}
