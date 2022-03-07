package com.jetpack.amountvisualtransformation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpack.amountvisualtransformation.ui.theme.AmountVisualTransformationTheme
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var amount by remember { mutableStateOf("") }

            AmountVisualTransformationTheme {
                Surface(color = MaterialTheme.colors.background) {
                   Scaffold(
                       topBar = {
                           TopAppBar(
                               title = {
                                   Text(
                                       text = "Visual Transformation",
                                       modifier = Modifier.fillMaxWidth(),
                                       textAlign = TextAlign.Center
                                   )
                               }
                           )
                       }
                   ) {
                       Column(
                           modifier = Modifier.fillMaxSize(),
                           horizontalAlignment = Alignment.CenterHorizontally,
                           verticalArrangement = Arrangement.Center
                       ) {
                           Text(
                               text = "Amount",
                               modifier = Modifier.padding(bottom = 10.dp),
                               fontWeight = FontWeight.Bold
                           )

                           OutlinedTextField(
                               value = amount,
                               onValueChange = {
                                   amount = it
                               },
                               keyboardOptions = KeyboardOptions(
                                   keyboardType = KeyboardType.Number
                               ),
                               visualTransformation = ThousandSeparatorVisualTransformation()
                           )
                       }
                   }
                }
            }
        }
    }
}

class ThousandSeparatorVisualTransformation(
    var maxFractionDigits: Int = Int.MAX_VALUE,
    var minFractionDigits: Int = 0
) : VisualTransformation {
    private val symbols = DecimalFormat().decimalFormatSymbols
    private val commaReplacementPattern = Regex("\\B(?=(?:\\d{3})+(?!\\d))")

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty())
            return TransformedText(text, OffsetMapping.Identity)

        val comma = symbols.groupingSeparator
        val dot = symbols.decimalSeparator
        val zero = symbols.zeroDigit

        var (intPart, fracPart) = text.text.split(dot)
            .let { Pair(it[0], it.getOrNull(1)) }

        val normalizedIntPart =
            if (intPart.isEmpty() && fracPart != null) zero.toString() else intPart

        val integersWithComma =
            normalizedIntPart.replace(commaReplacementPattern, comma.toString())

        val minFractionDigits = min(this.maxFractionDigits, this.minFractionDigits)
        if (minFractionDigits > 0 || !fracPart.isNullOrEmpty()) {
            if (fracPart == null)
                fracPart = ""

            fracPart = fracPart.take(maxFractionDigits).padEnd(minFractionDigits, zero)
        }

        val newText = AnnotatedString(
            integersWithComma + if (fracPart == null) "" else ".$fracPart",
            text.spanStyles,
            text.paragraphStyles
        )

        val offsetMapping = ThousandSeparatorOffsetMapping(
            intPart.length,
            integersWithComma.length,
            newText.length,
            integersWithComma.indices.filter { integersWithComma[it] == comma }.asSequence(),
            normalizedIntPart != intPart
        )

        return TransformedText(newText, offsetMapping)
    }

    private inner class ThousandSeparatorOffsetMapping(
        val originalIntegerLength: Int,
        val transformedIntegersLength: Int,
        val transformedLength: Int,
        val commaIndices: Sequence<Int>,
        addedLeadingZero: Boolean
    ) : OffsetMapping {
        val commaCount = calcCommaCount(originalIntegerLength)
        val leadingZeroOffset = if (addedLeadingZero) 1 else 0

        override fun originalToTransformed(offset: Int): Int =
            if (offset >= originalIntegerLength)
                if (offset - originalIntegerLength > maxFractionDigits)
                    transformedLength
                else
                    offset + commaCount + leadingZeroOffset
            else
                offset + (commaCount - calcCommaCount(originalIntegerLength - offset))

        override fun transformedToOriginal(offset: Int): Int =
            if (offset >= transformedIntegersLength)
                min(offset - commaCount, transformedLength) - leadingZeroOffset
            else
                offset - commaIndices.takeWhile { it <= offset }.count()

        private fun calcCommaCount(intDigitCount: Int) =
            max((intDigitCount - 1) / 3, 0)
    }
}
























