package itstep.learning.androidpv211;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CalcActivity extends AppCompatActivity {
    private TextView tvExpression;
    private TextView tvResult;
    private StringBuilder currentExpression = new StringBuilder();
    private boolean lastInputIsOperator = false;
    private boolean clearOnNextInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc);

        tvExpression = findViewById(R.id.calc_tv_expression);
        tvResult = findViewById(R.id.calc_tv_result);

        int[] digitIds = {
                R.id.calc_btn_0, R.id.calc_btn_1, R.id.calc_btn_2, R.id.calc_btn_3,
                R.id.calc_btn_4, R.id.calc_btn_5, R.id.calc_btn_6,
                R.id.calc_btn_7, R.id.calc_btn_8, R.id.calc_btn_9
        };

        for (int id : digitIds) {
            findViewById(id).setOnClickListener(this::onDigitClick);
        }

        findViewById(R.id.calc_btn_add).setOnClickListener(this::onOperatorClick);
        findViewById(R.id.calc_btn_sub).setOnClickListener(this::onOperatorClick);
        findViewById(R.id.calc_btn_mul).setOnClickListener(this::onOperatorClick);
        findViewById(R.id.calc_btn_div).setOnClickListener(this::onOperatorClick);

        findViewById(R.id.calc_btn_c).setOnClickListener(v -> clearAll());
        findViewById(R.id.calc_btn_eq).setOnClickListener(v -> onEqualsClick());
        findViewById(R.id.calc_btn_dot).setOnClickListener(this::onDotClick);
        findViewById(R.id.calc_btn_backspace).setOnClickListener(v -> {
            String current = tvResult.getText().toString();
            if (!current.isEmpty() && !current.equals("0")) {
                tvResult.setText(current.substring(0, current.length() - 1));
                if (tvResult.getText().toString().isEmpty()) {
                    tvResult.setText("0");
                }
            }
        });

        findViewById(R.id.calc_btn_sqr).setOnClickListener(v -> {
            try {
                double val = Double.parseDouble(tvResult.getText().toString());
                tvResult.setText(String.valueOf(val * val));
            } catch (Exception e) {
                tvResult.setText("Ошибка");
            }
        });

        findViewById(R.id.calc_btn_sqrt).setOnClickListener(v -> {
            try {
                double val = Double.parseDouble(tvResult.getText().toString());
                if (val < 0) {
                    tvResult.setText("Ошибка");
                } else {
                    tvResult.setText(String.valueOf(Math.sqrt(val)));
                }
            } catch (Exception e) {
                tvResult.setText("Ошибка");
            }
        });

        findViewById(R.id.calc_btn_ce).setOnClickListener(v -> {
            tvResult.setText("0");
        });

        findViewById(R.id.calc_btn_inv).setOnClickListener(v -> {
            try {
                double val = Double.parseDouble(tvResult.getText().toString());
                if (val == 0) {
                    tvResult.setText("Ошибка");
                } else {
                    tvResult.setText(String.valueOf(1 / val));
                }
            } catch (Exception e) {
                tvResult.setText("Ошибка");
            }
        });

        findViewById(R.id.calc_btn_pm).setOnClickListener(v -> {
            try {
                double val = Double.parseDouble(tvResult.getText().toString());
                tvResult.setText(String.valueOf(-val));
            } catch (Exception e) {
                tvResult.setText("Ошибка");
            }
        });

        findViewById(R.id.calc_btn_percent).setOnClickListener(v -> {
            try {
                double val = Double.parseDouble(tvResult.getText().toString());
                tvResult.setText(String.valueOf(val / 100));
            } catch (Exception e) {
                tvResult.setText("Ошибка");
            }
        });

        clearAll();
    }

    private void onDigitClick(View view) {
        if (clearOnNextInput) {
            tvResult.setText("");
            clearOnNextInput = false;
        }

        String digit = ((Button) view).getText().toString();
        tvResult.append(digit);
        lastInputIsOperator = false;
    }

    private void onOperatorClick(View view) {
        if (lastInputIsOperator) return;

        String operator = ((Button) view).getText().toString();
        currentExpression.append(tvResult.getText().toString()).append(" ").append(operator).append(" ");
        tvExpression.setText(currentExpression.toString());
        tvResult.setText("");
        lastInputIsOperator = true;
    }

    private void onDotClick(View view) {
        String current = tvResult.getText().toString();
        if (!current.contains(".")) {
            tvResult.append(".");
        }
    }

    private void onEqualsClick() {
        if (lastInputIsOperator) return;

        currentExpression.append(tvResult.getText().toString());
        String expression = currentExpression.toString();
        double result = evaluateExpression(expression);
        tvResult.setText(String.valueOf(result));
        tvExpression.setText(expression + " =");
        currentExpression.setLength(0);
        clearOnNextInput = true;
    }

    private double evaluateExpression(String expr) {
        try {
            String[] tokens = expr.split(" ");
            double result = Double.parseDouble(tokens[0]);
            for (int i = 1; i < tokens.length; i += 2) {
                String op = tokens[i];
                double next = Double.parseDouble(tokens[i + 1]);
                switch (op) {
                    case "+":
                        result += next;
                        break;
                    case "-":
                        result -= next;
                        break;
                    case "×":
                        result *= next;
                        break;
                    case "÷":
                        result /= next;
                        break;
                }
            }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }

    private void clearAll() {
        currentExpression.setLength(0);
        tvExpression.setText("");
        tvResult.setText("0");
        lastInputIsOperator = false;
    }
}
