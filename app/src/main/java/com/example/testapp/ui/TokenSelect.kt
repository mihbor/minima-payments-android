package com.example.testapp.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import ltd.mbor.minimak.Balance

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TokenSelect(enabled: Boolean, balances: Map<String, Balance>, tokenId: String, setTokenId: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
    OutlinedTextField(
      value = balances[tokenId]?.let { (it.tokenName ?: "Minima") + " [${it.sendable.toPlainString().take(12)}]" } ?: "",
      {},
      modifier = Modifier.fillMaxWidth(),
      readOnly = true,
      enabled = enabled,
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = expanded
        )
      }
    )
    ExposedDropdownMenu(expanded, { expanded = false }) {
      balances.values.forEach {
        DropdownMenuItem(enabled = enabled && it.sendable > BigDecimal.ZERO, onClick = {
          setTokenId(it.tokenId)
        }) {
          Text(it.tokenName ?: "Minima")
          Text(" [${it.sendable.toPlainString()}]")
        }
      }
    }
  }
}