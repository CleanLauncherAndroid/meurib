package com.example.data

import com.example.model.AffixInfo
import com.example.model.IrabCategory
import com.example.model.MorphReading
import com.example.model.SentenceIrabResult
import com.example.model.WordIrabResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class AnalysisRepository(
    private val dao: SentenceAnalysisDao
) {

    val allAnalyses: Flow<List<SentenceIrabResult>> =
        dao.getAllAnalyses().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    suspend fun saveAnalysis(result: SentenceIrabResult): Long {
        val entity = SentenceAnalysisEntity(
            sentence = result.sentence,
            sentenceType = result.sentenceType,
            tokensJson = serializeWords(result.words),
            createdAt = result.createdAt,
            isPinned = result.isPinned,
            layer2Used = result.layer2Used
        )
        return dao.insertAnalysis(entity)
    }

    suspend fun togglePinned(id: Long, pinned: Boolean) {
        dao.updatePinned(id, pinned)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun SentenceAnalysisEntity.toDomain(): SentenceIrabResult {
        return SentenceIrabResult(
            id = id,
            sentence = sentence,
            sentenceType = sentenceType,
            words = deserializeWords(tokensJson),
            createdAt = createdAt,
            isPinned = isPinned,
            layer2Used = layer2Used
        )
    }

    private fun serializeWords(words: List<WordIrabResult>): String {
        val arr = JSONArray()
        for (w in words) {
            val obj = JSONObject().apply {
                put("word", w.word)
                put("diacritizedWord", w.diacritizedWord)
                put("partOfSpeech", w.partOfSpeech)
                put("syntacticRole", w.syntacticRole)
                put("grammaticalState", w.grammaticalState)
                put("fullIrab", w.fullIrab)
                put("category", w.category.name)
                put("lemma", w.lemma)
                put("root", w.root ?: JSONObject.NULL)
                put("stem", w.stem)
                put("pattern", w.pattern ?: JSONObject.NULL)
                put("subType", w.subType ?: JSONObject.NULL)
                put("tense", w.tense ?: JSONObject.NULL)
                put("gender", w.gender ?: JSONObject.NULL)
                put("number", w.number ?: JSONObject.NULL)
                put("proclitics", serializeAffixes(w.proclitics))
                put("enclitics", serializeAffixes(w.enclitics))
                put("allReadings", serializeReadings(w.allReadings))
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun serializeAffixes(affixes: List<AffixInfo>): JSONArray {
        val arr = JSONArray()
        for (a in affixes) {
            arr.put(JSONObject().apply {
                put("form", a.form)
                put("description", a.description ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun serializeReadings(readings: List<MorphReading>): JSONArray {
        val arr = JSONArray()
        for (r in readings.take(6)) {
            arr.put(JSONObject().apply {
                put("diacritized", r.diacritized)
                put("lemma", r.lemma)
                put("stem", r.stem)
                put("root", r.root ?: JSONObject.NULL)
                put("pattern", r.pattern ?: JSONObject.NULL)
                put("partOfSpeech", r.partOfSpeech)
                put("subType", r.subType ?: JSONObject.NULL)
                put("tense", r.tense ?: JSONObject.NULL)
                put("gender", r.gender ?: JSONObject.NULL)
                put("number", r.number ?: JSONObject.NULL)
                put("person", r.person ?: JSONObject.NULL)
                put("pronoun", r.pronoun ?: JSONObject.NULL)
                put("voice", r.voice ?: JSONObject.NULL)
                put("state", r.state ?: JSONObject.NULL)
                put("caseOrMood", r.caseOrMood ?: JSONObject.NULL)
                put("proclitics", serializeAffixes(r.proclitics))
                put("enclitics", serializeAffixes(r.enclitics))
                put("score", r.score)
            })
        }
        return arr
    }

    private fun deserializeWords(json: String): List<WordIrabResult> {
        val arr = JSONArray(json)
        val result = ArrayList<WordIrabResult>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val catName = obj.optString("category", IrabCategory.OTHER.name)
            val category = runCatching { IrabCategory.valueOf(catName) }.getOrDefault(IrabCategory.OTHER)
            result.add(
                WordIrabResult(
                    word = obj.optString("word"),
                    diacritizedWord = obj.optString("diacritizedWord"),
                    partOfSpeech = obj.optString("partOfSpeech"),
                    syntacticRole = obj.optString("syntacticRole"),
                    grammaticalState = obj.optString("grammaticalState"),
                    fullIrab = obj.optString("fullIrab"),
                    category = category,
                    lemma = obj.optString("lemma"),
                    root = obj.optNullableString("root"),
                    stem = obj.optString("stem"),
                    pattern = obj.optNullableString("pattern"),
                    subType = obj.optNullableString("subType"),
                    tense = obj.optNullableString("tense"),
                    gender = obj.optNullableString("gender"),
                    number = obj.optNullableString("number"),
                    proclitics = deserializeAffixes(obj.optJSONArray("proclitics")),
                    enclitics = deserializeAffixes(obj.optJSONArray("enclitics")),
                    allReadings = deserializeReadings(obj.optJSONArray("allReadings"))
                )
            )
        }
        return result
    }

    private fun deserializeAffixes(arr: JSONArray?): List<AffixInfo> {
        if (arr == null) return emptyList()
        val list = ArrayList<AffixInfo>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(AffixInfo(o.optString("form"), o.optNullableString("description")))
        }
        return list
    }

    private fun deserializeReadings(arr: JSONArray?): List<MorphReading> {
        if (arr == null) return emptyList()
        val list = ArrayList<MorphReading>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                MorphReading(
                    diacritized = o.optString("diacritized"),
                    lemma = o.optString("lemma"),
                    stem = o.optString("stem"),
                    root = o.optNullableString("root"),
                    pattern = o.optNullableString("pattern"),
                    partOfSpeech = o.optString("partOfSpeech"),
                    subType = o.optNullableString("subType"),
                    tense = o.optNullableString("tense"),
                    gender = o.optNullableString("gender"),
                    number = o.optNullableString("number"),
                    person = o.optNullableString("person"),
                    pronoun = o.optNullableString("pronoun"),
                    voice = o.optNullableString("voice"),
                    state = o.optNullableString("state"),
                    caseOrMood = o.optNullableString("caseOrMood"),
                    proclitics = deserializeAffixes(o.optJSONArray("proclitics")),
                    enclitics = deserializeAffixes(o.optJSONArray("enclitics")),
                    score = o.optDouble("score", 0.0)
                )
            )
        }
        return list
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }
}
