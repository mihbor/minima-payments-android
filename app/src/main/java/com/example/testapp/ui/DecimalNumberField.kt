package com.example.testapp.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal

@Composable
fun DecimalNumberField(value: BigDecimal, enabled: Boolean, setValue: (BigDecimal) -> Unit) {
  var text by remember { mutableStateOf(value.toPlainString()) }
  value.takeUnless { it == text.toBigDecimalOrNull() || it == BigDecimal.ZERO && text.isEmpty()} ?.let { text = it.toPlainString() }
  OutlinedTextField(
    value = text,
    onValueChange = {
      if (it.isEmpty()) setValue(BigDecimal.ZERO)
      else it.toBigDecimalOrNull()?.let{ setValue(it) }
      text = it
    },
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
  )
}

fun String.toBigDecimalOrNull(): BigDecimal? {
  return try {
    toBigDecimal()
  } catch (e: Exception) {
    null
  }
}
