package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.calculator.ui.theme.CalculatorTheme
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF111111)
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

private enum class Operator(val symbol: String) {
    Add("+"),
    Subtract("−"),
    Multiply("×"),
    Divide("÷")
}

private data class CalculatorState(
    val currentInput: String = "0",
    val storedValue: BigDecimal? = null,
    val pendingOperator: Operator? = null,
    val resetInputOnNextDigit: Boolean = false
)

@Composable
private fun CalculatorScreen() {
    var state by rememberSaveable { mutableStateOf(CalculatorState()) }

    val rows = listOf(
        listOf("AC", "+/-", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = state.currentInput,
            color = Color.White,
            fontSize = 64.sp,
            lineHeight = 72.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { label ->
                    val isWide = label == "0"
                    CalculatorButton(
                        label = label,
                        modifier = if (isWide) Modifier.width(172.dp) else Modifier.size(80.dp),
                        backgroundColor = when (label) {
                            "AC", "+/-", "%" -> Color(0xFFA5A5A5)
                            "÷", "×", "−", "+", "=" -> Color(0xFFFF9F0A)
                            else -> Color(0xFF333333)
                        },
                        textColor = if (label in listOf("AC", "+/-", "%")) Color.Black else Color.White
                    ) {
                        state = reduce(state, label)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorButton(
    label: String,
    modifier: Modifier,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun reduce(state: CalculatorState, input: String): CalculatorState {
    return when (input) {
        "AC" -> CalculatorState()
        "+/-" -> state.copy(
            currentInput = if (state.currentInput == "0") "0"
            else if (state.currentInput.startsWith("-")) state.currentInput.drop(1)
            else "-${state.currentInput}"
        )

        "%" -> {
            val value = state.currentInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
            state.copy(currentInput = format(value.divide(BigDecimal(100))))
        }

        "." -> {
            if (state.resetInputOnNextDigit) {
                state.copy(currentInput = "0.", resetInputOnNextDigit = false)
            } else if (state.currentInput.contains(".")) {
                state
            } else {
                state.copy(currentInput = "${state.currentInput}.")
            }
        }

        "+", "−", "×", "÷" -> {
            val nextOperator = mapOperator(input)
            val current = state.currentInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val stored = state.storedValue

            if (stored != null && state.pendingOperator != null && !state.resetInputOnNextDigit) {
                val result = calculate(stored, current, state.pendingOperator)
                state.copy(
                    currentInput = format(result),
                    storedValue = result,
                    pendingOperator = nextOperator,
                    resetInputOnNextDigit = true
                )
            } else {
                state.copy(
                    storedValue = current,
                    pendingOperator = nextOperator,
                    resetInputOnNextDigit = true
                )
            }
        }

        "=" -> {
            val stored = state.storedValue
            val op = state.pendingOperator
            val current = state.currentInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (stored == null || op == null) {
                state
            } else {
                val result = calculate(stored, current, op)
                state.copy(
                    currentInput = format(result),
                    storedValue = null,
                    pendingOperator = null,
                    resetInputOnNextDigit = true
                )
            }
        }

        else -> {
            if (state.resetInputOnNextDigit) {
                state.copy(currentInput = input, resetInputOnNextDigit = false)
            } else {
                val next = if (state.currentInput == "0") input else state.currentInput + input
                state.copy(currentInput = next.take(14))
            }
        }
    }
}

private fun mapOperator(symbol: String): Operator = when (symbol) {
    "+" -> Operator.Add
    "−" -> Operator.Subtract
    "×" -> Operator.Multiply
    else -> Operator.Divide
}

private fun calculate(left: BigDecimal, right: BigDecimal, op: Operator): BigDecimal = when (op) {
    Operator.Add -> left + right
    Operator.Subtract -> left - right
    Operator.Multiply -> left * right
    Operator.Divide -> if (right.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
    else left.divide(right, 10, RoundingMode.HALF_UP)
}

private fun format(value: BigDecimal): String {
    val normalized = value.stripTrailingZeros()
    return if (normalized.scale() <= 0) normalized.toPlainString()
    else normalized.toPlainString().take(14)
}
