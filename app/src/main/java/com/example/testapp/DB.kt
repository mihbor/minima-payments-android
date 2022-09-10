import com.example.testapp.ChannelState
import com.example.testapp.minima.MDS
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat

suspend fun createDB() {
  MDS.sql(//"""DROP TABLE IF EXISTS channel;
    """CREATE TABLE IF NOT EXISTS channel(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR,
    my_balance DECIMAL(20,10),
    other_balance DECIMAL(20,10),
    my_address VARCHAR,
    other_address VARCHAR,
    my_trigger_key VARCHAR,
    my_update_key VARCHAR,
    my_settle_key VARCHAR,
    other_trigger_key VARCHAR,
    other_update_key VARCHAR,
    other_settle_key VARCHAR,
    sequence_number INT,
    time_lock INT,
    trigger_tx VARCHAR,
    update_tx VARCHAR,
    settle_tx VARCHAR,
    multisig_address VARCHAR,
    eltoo_address VARCHAR
  );""".trimMargin())
}

suspend fun getChannel(eltooAddress: String): ChannelState? {
  val sql = MDS.sql("SELECT * FROM channel WHERE eltoo_address = '$eltooAddress';")?.jsonObject
  val rows = sql?.get("rows")?.jsonArray ?: emptyList()

  return rows.firstOrNull()?.jsonObject?.toChannel()
}

suspend fun getChannels(status: String? = null): List<ChannelState> {
  val sql = MDS.sql("SELECT * FROM channel${status?.let { " WHERE status = '$it'" } ?: ""} ORDER BY id DESC;")?.jsonObject
  val rows = sql?.get("rows")?.jsonArray ?: emptyList()
  
  return rows.map{it.jsonObject}.map(JsonObject::toChannel)
}

private fun JsonObject.toChannel() = ChannelState(
  id = (this["ID"]!!.jsonPrimitive.content).toInt(),
  sequenceNumber = (this["SEQUENCE_NUMBER"]!!.jsonPrimitive.content).toInt(),
  status = this["STATUS"]!!.jsonPrimitive.content,
  myBalance = (this["MY_BALANCE"]!!.jsonPrimitive.content).toBigDecimal(),
  counterPartyBalance = (this["OTHER_BALANCE"]!!.jsonPrimitive.content).toBigDecimal(),
  myAddress = this["MY_ADDRESS"]!!.jsonPrimitive.content,
  myTriggerKey = this["MY_TRIGGER_KEY"]!!.jsonPrimitive.content,
  myUpdateKey = this["MY_UPDATE_KEY"]!!.jsonPrimitive.content,
  mySettleKey = this["MY_SETTLE_KEY"]!!.jsonPrimitive.content,
  counterPartyAddress = this["OTHER_ADDRESS"]!!.jsonPrimitive.content,
  counterPartyTriggerKey = this["OTHER_TRIGGER_KEY"]!!.jsonPrimitive.content,
  counterPartyUpdateKey = this["OTHER_UPDATE_KEY"]!!.jsonPrimitive.content,
  counterPartySettleKey = this["OTHER_SETTLE_KEY"]!!.jsonPrimitive.content,
  triggerTx = this["TRIGGER_TX"]!!.jsonPrimitive.content,
  updateTx = this["UPDATE_TX"]!!.jsonPrimitive.content,
  settlementTx = this["SETTLE_TX"]!!.jsonPrimitive.content,
  timeLock = (this["TIME_LOCK"]!!.jsonPrimitive.content).toInt(),
  eltooAddress = this["ELTOO_ADDRESS"]!!.jsonPrimitive.content,
  updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS").parse(this["UPDATED_AT"]!!.jsonPrimitive.content).time
)

suspend fun updateChannelStatus(channel: ChannelState, status: String): ChannelState {
  MDS.sql("""UPDATE channel SET
    status = '$status',
    updated_at = NOW()
    WHERE id = ${channel.id};
  """)
  return channel.copy(status = status, updatedAt = System.currentTimeMillis())
}

suspend fun setChannelOpen(multisigAddress: String) {
  MDS.sql("""UPDATE channel SET
    updated_at = NOW(),
    status = 'OPEN'
    WHERE multisig_address = '$multisigAddress'
    AND status = 'OFFERED';
  """)
}

suspend fun updateChannel(
  channel: ChannelState,
  channelBalance: Pair<BigDecimal, BigDecimal>,
  sequenceNumber: Int,
  updateTx: String,
  settlementTx: String
): ChannelState {
  MDS.sql("""UPDATE channel SET
    my_balance = ${channelBalance.first.toPlainString()},
    other_balance = ${channelBalance.second.toPlainString()},
    sequence_number = $sequenceNumber,
    update_tx = '$updateTx',
    settle_tx = '$settlementTx',
    updated_at = NOW()
    WHERE id = ${channel.id};
  """)
  return channel.copy(
    myBalance = channelBalance.first,
    counterPartyBalance = channelBalance.second,
    sequenceNumber = sequenceNumber,
    updateTx = updateTx,
    settlementTx = settlementTx,
    updatedAt = System.currentTimeMillis()
  )
}