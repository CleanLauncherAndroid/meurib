package com.example.engine

import com.example.model.IrabCategory
import com.example.model.MorphReading
import com.example.model.RawSentenceAnalysis
import com.example.model.RawTokenAnalysis
import com.example.model.SentenceIrabResult
import com.example.model.WordIrabResult

/**
 * محرك القواعد النحوية العربية الموسّع:
 * يعتمد على مخرجات التحليل الصرفي من مكتبة farahidi
 * (part_of_speech, case_or_mood, tense, voice, root, proclitics, inflections)
 * لتحديد نوع الجملة والموقع النحوي وإعرابها الكامل، بما في ذلك:
 * الفاعل، المفعول به، نائب الفاعل، المبتدأ والخبر، إنّ وكان، الأفعال الخمسة،
 * النداء، ظروف الزمان والمكان، المفعول المطلق، الممنوع من الصرف، المضاف إليه والنعت.
 */
object ArabicGrammarEngine {

    private val DIACRITICS_REGEX = Regex("[\u064B-\u0652\u0670]")

    private val PREPOSITIONS = setOf(
        "من", "إلى", "الى", "عن", "على", "في", "حتى", "منذ", "مذ", "رب", "خلا", "عدا", "حاشا"
    )

    private val CONJUNCTIONS = setOf("و", "ثم", "أو", "او", "أم", "ام", "بل", "لكن")
    private val VOCATIVE_PARTICLES = setOf("يا", "أيا", "ايا", "هيا", "أي", "أيتها", "أيها")

    private val SUBJUNCTIVE_PARTICLES = setOf("أن", "لن", "كي", "لكي", "حتى", "إذن")
    private val JUSSIVE_PARTICLES = setOf("لم", "لما", "لأم")
    private val NEGATION_PARTICLES = setOf("لا", "ما", "لن", "لم")

    private val INNA_SISTERS = setOf(
        "إن", "ان", "أن", "كأن", "لكن", "ليت", "لعل"
    )

    private val KANA_SISTERS_BARE = setOf(
        "كان", "أصبح", "اصبح", "أمسى", "امسى", "أضحى", "اضحى",
        "ظل", "بات", "صار", "ليس", "زال", "برح", "فتئ", "انفك",
        "يكون", "يصبح", "يمسي", "يظل", "يبيت", "يصير", "تكون", "تصبح"
    )

    private val PRONOUNS = setOf(
        "أنا", "انا", "نحن", "أنت", "انت", "أنتما", "انتما", "أنتم", "انتم", "أنتن", "انتن",
        "هو", "هي", "هما", "هم", "هن"
    )

    private val DEMONSTRATIVES = setOf(
        "هذا", "هذه", "هذان", "هذين", "هاتان", "هاتين", "هؤلاء",
        "ذلك", "تلك", "أولئك", "اولئك", "هنا", "هناك"
    )

    private val RELATIVES = setOf(
        "الذي", "التي", "اللذان", "اللذين", "اللتان", "اللتين", "الذين", "اللاتي", "اللائي"
    )

    private val TIME_ADVERBS = setOf(
        "صباحا", "صباحاً", "مساء", "مساءً", "ليلا", "ليلاً", "نهارا", "نهاراً",
        "فجرا", "فجراً", "ظهرا", "ظهراً", "عصرا", "عصراً", "مغربا", "عشاء",
        "اليوم", "أمس", "امس", "غدا", "غداً", "الآن", "الان", "حين", "وقت",
        "أثناء", "اثناء", "قبل", "بعد", "باكرا", "باكراً", "ساعة", "يوما", "يوماً"
    )

    private val PLACE_ADVERBS = setOf(
        "أمام", "امام", "خلف", "وراء", "فوق", "تحت", "يمين", "شمال", "يسار",
        "بين", "عند", "حول", "داخل", "خارج", "وسط", "تجاه", "نحو", "قرب", "جانب"
    )

    private val MABNI_ADVERBS = setOf("أمس", "امس", "الآن", "الان", "حيث")

    private val DIPTOTE_WORDS = setOf(
        "مساجد", "مدارس", "معاهد", "مصابيح", "مفاتيح", "شوارع", "حدائق", "قواعد",
        "عصافير", "أحمد", "احمد", "عمر", "إبراهيم", "ابراهيم", "يوسف", "يعقوب",
        "فاطمة", "مريم", "زينب", "خديجة", "عائشة", "مكة", "بغداد", "دمشق",
        "صحراء", "بيضاء", "حمراء", "خضراء", "أفضل", "افضل", "أكبر", "اكبر", "أجمل", "اجمل"
    )

    private val PAST_TRANSITIVE_HINTS = setOf(
        "كتب", "قرأ", "شرب", "أكل", "فتح", "ضرب", "نصر", "علم", "فهم",
        "حفظ", "سمع", "رأى", "أخذ", "أعطى", "صنع", "بنى", "زرع", "حصد",
        "شرح", "درس", "تعلم", "اشترى", "باع", "طلب", "ساعد", "رفع", "كسر", "خلق"
    )

    fun stripDiacritics(word: String): String = DIACRITICS_REGEX.replace(word, "")

    fun analyzeSentence(
        raw: RawSentenceAnalysis,
        detailedSigns: Boolean = true
    ): SentenceIrabResult {
        val tokens = raw.tokens
        if (tokens.isEmpty()) {
            return SentenceIrabResult(
                sentence = raw.sentence,
                sentenceType = "جملة غير مكتملة",
                words = emptyList(),
                layer2Used = raw.layer2Used
            )
        }

        val selectedReadings = tokens.mapIndexed { index, token ->
            selectBestReading(token, index, tokens)
        }

        val firstSignificantIdx = findFirstSignificantIndex(tokens, selectedReadings)
        val firstReading = selectedReadings.getOrNull(firstSignificantIdx)
        val firstTokenBare = tokens.getOrNull(firstSignificantIdx)?.bareWord ?: ""

        val startsWithInna = isInnaSister(
            tokens.getOrNull(firstSignificantIdx)?.originalWord ?: "",
            firstTokenBare
        )
        val startsWithVocative = firstTokenBare in VOCATIVE_PARTICLES
        val startsWithSubjOrJuss = firstTokenBare in SUBJUNCTIVE_PARTICLES || firstTokenBare in JUSSIVE_PARTICLES

        val isVerbalSentence = when {
            startsWithInna -> false
            firstReading?.partOfSpeech == "فعل" -> true
            startsWithSubjOrJuss && selectedReadings.getOrNull(firstSignificantIdx + 1)?.partOfSpeech == "فعل" -> true
            startsWithVocative && selectedReadings.getOrNull(firstSignificantIdx + 2)?.partOfSpeech == "فعل" -> true
            else -> false
        }

        val sentenceType = when {
            startsWithVocative && !isVerbalSentence -> "أسلوب نداء"
            isVerbalSentence -> "جملة فعلية"
            firstReading?.partOfSpeech == "اسم" || startsWithInna ||
                firstTokenBare in PRONOUNS || firstTokenBare in DEMONSTRATIVES -> "جملة اسمية"
            else -> "تركيب نحوي"
        }

        val results = ArrayList<WordIrabResult>(tokens.size)
        var afterPreposition = false
        var afterConjunction = false
        var afterVocative = false
        var afterAdverb = false
        var afterSubjunctive = false
        var afterJussive = false
        var afterInna = false
        var innaSubjectAssigned = false
        var inKanaClause = false
        var kanaSubjectAssigned = false
        var verbSeen = false
        var currentVerbIsPassive = false
        var currentVerbRoot: String? = null
        var subjectAssigned = false
        var objectAssigned = false
        var mubtadaAssigned = false
        var khabarAssigned = false
        var lastNounIrabCategory: IrabCategory? = null
        var lastNounWasIndefinite = false
        var lastNounWasDefinite = false

        for (i in tokens.indices) {
            val token = tokens[i]
            val reading = selectedReadings[i]
            val bare = token.bareWord
            val explicitHaraka = token.explicitHaraka
            val pos = reading?.partOfSpeech ?: token.partOfSpeech
            val subType = reading?.subType
            val tense = reading?.tense
            val hasAlPrefix = bare.startsWith("ال") && bare.length > 3
            val hasAttachedPrep = reading?.proclitics?.any {
                it.description?.contains("الجر") == true
            } == true

            // 1. Vocative particle (يا / أيا / هيا)
            if (bare in VOCATIVE_PARTICLES) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف نداء",
                        role = "حرف نداء",
                        state = "مبني على السكون لا محل له من الإعراب",
                        fullIrab = "حرف نداء مبني على السكون لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterVocative = true
                afterPreposition = false
                afterConjunction = false
                afterAdverb = false
                continue
            }

            // 2. إنّ وأخواتها
            if (isInnaSister(token.originalWord, bare)) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف ناسخ",
                        role = "حرف مشبّه بالفعل",
                        state = "مبني على الفتح لا محل له من الإعراب",
                        fullIrab = "حرف توكيد ونصب مبني على الفتح لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterInna = true
                innaSubjectAssigned = false
                afterPreposition = false
                afterConjunction = false
                afterAdverb = false
                continue
            }

            // 3. Prepositions
            if (bare in PREPOSITIONS || (pos == "حرف" && subType == "جر")) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف جر",
                        role = "حرف جر",
                        state = "مبني لا محل له من الإعراب",
                        fullIrab = "حرف جر مبني على السكون لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterPreposition = true
                afterConjunction = false
                afterAdverb = false
                continue
            }

            // 4. Conjunctions
            if (bare in CONJUNCTIONS || (pos == "حرف" && subType == "عطف")) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف عطف",
                        role = "حرف عطف",
                        state = "مبني لا محل له من الإعراب",
                        fullIrab = "حرف عطف مبني لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterConjunction = true
                afterPreposition = false
                afterAdverb = false
                continue
            }

            // 5. Subjunctive / Jussive / Negation
            if (bare in SUBJUNCTIVE_PARTICLES) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف نصب",
                        role = "أداة نصب",
                        state = "مبني لا محل له من الإعراب",
                        fullIrab = "حرف نصب ومصدرية مبني على السكون لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterSubjunctive = true
                afterPreposition = false
                continue
            }

            if (bare in JUSSIVE_PARTICLES) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف جزم",
                        role = "أداة جزم",
                        state = "مبني لا محل له من الإعراب",
                        fullIrab = "حرف نفي وجزم وقلب مبني على السكون لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterJussive = true
                afterPreposition = false
                continue
            }

            if (bare in NEGATION_PARTICLES) {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "حرف نفي",
                        role = "أداة نفي",
                        state = "مبني لا محل له من الإعراب",
                        fullIrab = "حرف نفي مبني لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterPreposition = false
                continue
            }

            if (pos == "حرف") {
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = if (!subType.isNullOrBlank()) "حرف ($subType)" else "حرف",
                        role = "حرف",
                        state = "مبني لا محل له من الإعراب",
                        fullIrab = "حرف مبني لا محل له من الإعراب",
                        category = IrabCategory.PARTICLE
                    )
                )
                afterPreposition = false
                continue
            }

            // 6. Verbs (incl. المبني للمجهول و الأفعال الخمسة و كان وأخواتها)
            if (pos == "فعل") {
                verbSeen = true
                afterPreposition = false
                afterConjunction = false
                afterAdverb = false
                afterVocative = false
                currentVerbRoot = reading?.root ?: token.contextRoot

                val isKanaSister = bare in KANA_SISTERS_BARE ||
                    (reading?.lemma?.let { stripDiacritics(it) in KANA_SISTERS_BARE } == true)
                if (isKanaSister) {
                    inKanaClause = true
                    kanaSubjectAssigned = false
                }

                val isPassive = !isKanaSister && isPassiveVerb(token.originalWord, reading)
                currentVerbIsPassive = isPassive
                if (isPassive) {
                    subjectAssigned = false // نائب فاعل pending
                }

                val isFiveVerbs = isFiveVerbForm(tense, bare, afterSubjunctive || afterJussive)
                val hasAttachedSubjectPronoun = isFiveVerbs || hasAttachedVerbSubject(reading, bare)

                val (displayVerbPos, verbRole, verbState, verbFull, verbCat) = when (tense) {
                    "ماض" -> {
                        val role = when {
                            isKanaSister -> "فعل ماضٍ ناسخ"
                            isPassive -> "فعل ماضٍ مبني للمجهول"
                            else -> "فعل ماضٍ"
                        }
                        val state = "مبني على الفتح"
                        val full = when {
                            isKanaSister -> "فعل ماضٍ ناسخ ناقص مبني على الفتح"
                            isPassive -> "فعل ماضٍ مبني للمجهول مبني على الفتح الظاهر على آخره"
                            else -> "فعل ماضٍ مبني على الفتح الظاهر على آخره"
                        }
                        FiveTuple("فعل ماضٍ", role, state, full, IrabCategory.VERB_MABNI)
                    }
                    "أمر" -> {
                        FiveTuple(
                            "فعل أمر",
                            "فعل أمر",
                            "مبني على السكون",
                            "فعل أمر مبني على السكون الظاهر على آخره",
                            IrabCategory.VERB_MABNI
                        )
                    }
                    else -> {
                        // مضارع (مع فحص الأفعال الخمسة والمبني للمجهول)
                        val baseRole = when {
                            isKanaSister -> "فعل مضارع ناسخ"
                            isPassive -> "فعل مضارع مبني للمجهول"
                            else -> "فعل مضارع"
                        }
                        val displayPos = if (isFiveVerbs) "فعل مضارع (من الأفعال الخمسة)" else "فعل مضارع"
                        when {
                            isFiveVerbs && afterSubjunctive -> FiveTuple(
                                displayPos,
                                baseRole,
                                if (detailedSigns) "منصوب وعلامة نصبه حذف النون لأنه من الأفعال الخمسة" else "منصوب (حذف النون)",
                                "$baseRole منصوب وعلامة نصبه حذف النون لأنه من الأفعال الخمسة، والضمير المتصل في محل رفع فاعل",
                                IrabCategory.MANSOUB
                            )
                            isFiveVerbs && afterJussive -> FiveTuple(
                                displayPos,
                                baseRole,
                                if (detailedSigns) "مجزوم وعلامة جزمه حذف النون لأنه من الأفعال الخمسة" else "مجزوم (حذف النون)",
                                "$baseRole مجزوم وعلامة جزمه حذف النون لأنه من الأفعال الخمسة، والضمير المتصل في محل رفع فاعل",
                                IrabCategory.VERB_MABNI
                            )
                            isFiveVerbs -> FiveTuple(
                                displayPos,
                                baseRole,
                                if (detailedSigns) "مرفوع وعلامة رفعه ثبوت النون لأنه من الأفعال الخمسة" else "مرفوع (ثبوت النون)",
                                "$baseRole مرفوع وعلامة رفعه ثبوت النون لأنه من الأفعال الخمسة، والضمير المتصل في محل رفع فاعل",
                                IrabCategory.MARFOU
                            )
                            afterSubjunctive || explicitHaraka == "منصوب" -> FiveTuple(
                                displayPos,
                                baseRole,
                                if (detailedSigns) "منصوب وعلامة نصبه الفتحة" else "منصوب (الفتحة)",
                                "$baseRole منصوب وعلامة نصبه الفتحة الظاهرة على آخره",
                                IrabCategory.MANSOUB
                            )
                            afterJussive || explicitHaraka == "مجزوم_أو_مبني" -> FiveTuple(
                                displayPos,
                                baseRole,
                                if (detailedSigns) "مجزوم وعلامة جزمه السكون" else "مجزوم (السكون)",
                                "$baseRole مجزوم وعلامة جزمه السكون الظاهر على آخره",
                                IrabCategory.VERB_MABNI
                            )
                            else -> FiveTuple(
                                displayPos,
                                baseRole,
                                if (detailedSigns) "مرفوع وعلامة رفعه الضمة" else "مرفوع (الضمة)",
                                "$baseRole مرفوع لتجرده عن الناصب والجازم وعلامة رفعه الضمة الظاهرة على آخره",
                                IrabCategory.MARFOU
                            )
                        }
                    }
                }

                afterSubjunctive = false
                afterJussive = false

                if (hasAttachedSubjectPronoun && !isKanaSister) {
                    subjectAssigned = true
                }

                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayVerbPos,
                        role = verbRole,
                        state = verbState,
                        fullIrab = verbFull,
                        category = verbCat
                    )
                )
                continue
            }

            // 7. Nouns / Adverbs / Vocative / Pronouns / Demonstratives
            val isPronoun = bare in PRONOUNS
            val isDemonstrative = bare in DEMONSTRATIVES
            val isRelative = bare in RELATIVES
            val displayNounPos = when {
                isPronoun -> "اسم (ضمير منفصل)"
                isDemonstrative -> "اسم إشارة"
                isRelative -> "اسم موصول"
                !subType.isNullOrBlank() -> "اسم ($subType)"
                else -> "اسم"
            }

            val isDefiniteNoun = hasAlPrefix || isPronoun || isDemonstrative || isRelative || subType == "اسم علم"
            val isMabniNoun = isPronoun || isDemonstrative || isRelative

            // 7a. Vocative noun (منادى)
            if (afterVocative) {
                val hasNextMudaf = i + 1 < tokens.size &&
                    (tokens[i + 1].bareWord.startsWith("ال") || tokens[i + 1].explicitHaraka == "مجرور") &&
                    !hasAlPrefix && explicitHaraka != "منصوب"
                val isMansoubVocative = explicitHaraka == "منصوب" || hasNextMudaf
                val role = "منادى"
                val state = if (isMansoubVocative) {
                    if (detailedSigns) "منصوب وعلامة نصبه الفتحة الظاهرة" else "منصوب (الفتحة)"
                } else {
                    "مبني على الضم في محل نصب"
                }
                val full = if (isMansoubVocative) {
                    "منادى مضاف منصوب وعلامة نصبه الفتحة الظاهرة على آخره"
                } else {
                    "منادى مفرد مبني على الضم في محل نصب على النداء"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = role,
                        state = state,
                        fullIrab = full,
                        category = IrabCategory.MANSOUB
                    )
                )
                afterVocative = false
                lastNounIrabCategory = IrabCategory.MANSOUB
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            // 7b. Prepositional phrase (standalone or attached ب/ل/ك)
            if (afterPreposition || hasAttachedPrep) {
                val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, i, tokens)
                val majrourSign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                val role = if (hasAttachedPrep) "اسم مجرور بحرف الجر المتصل" else "اسم مجرور"
                val state = if (isMabniNoun) {
                    "مبني في محل جر بحرف الجر"
                } else {
                    if (detailedSigns) "مجرور وعلامة جره $majrourSign" else "مجرور ($majrourSign)"
                }
                val full = if (isMabniNoun) {
                    "$displayNounPos مبني في محل جر بحرف الجر"
                } else {
                    "$role وعلامة جره $majrourSign"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = role,
                        state = state,
                        fullIrab = full,
                        category = IrabCategory.MAJROUR
                    )
                )
                afterPreposition = false
                afterConjunction = false
                afterAdverb = false
                lastNounIrabCategory = IrabCategory.MAJROUR
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            // 7c. Noun directly after place/time adverb -> مضاف إليه
            if (afterAdverb) {
                val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, i, tokens)
                val majrourSign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "مضاف إليه",
                        state = if (detailedSigns) "مجرور وعلامة جره $majrourSign" else "مجرور ($majrourSign)",
                        fullIrab = "مضاف إليه مجرور وعلامة جره $majrourSign",
                        category = IrabCategory.MAJROUR
                    )
                )
                afterAdverb = false
                lastNounIrabCategory = IrabCategory.MAJROUR
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            // 7d. Time & Place Adverbs (ظرف زمان / ظرف مكان)
            val bareNorm = stripDiacritics(token.originalWord)
            val isTimeAdv = bareNorm in TIME_ADVERBS || token.originalWord in TIME_ADVERBS
            val isPlaceAdv = bareNorm in PLACE_ADVERBS
            if ((isTimeAdv || isPlaceAdv) && (verbSeen || mubtadaAssigned)) {
                val advRole = if (isTimeAdv) "ظرف زمان" else "ظرف مكان"
                val isMabniAdv = bareNorm in MABNI_ADVERBS
                val advState = if (isMabniAdv) {
                    "مبني في محل نصب مفعول فيه"
                } else {
                    if (detailedSigns) "منصوب وعلامة نصبه الفتحة الظاهرة" else "منصوب (الفتحة)"
                }
                val advFull = if (isMabniAdv) {
                    "$advRole مبني في محل نصب على الظرفية"
                } else {
                    "$advRole (مفعول فيه) منصوب وعلامة نصبه الفتحة الظاهرة"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = "اسم ($advRole)",
                        role = advRole,
                        state = advState,
                        fullIrab = advFull,
                        category = IrabCategory.MANSOUB
                    )
                )
                val nextBare = tokens.getOrNull(i + 1)?.bareWord
                afterAdverb = isPlaceAdv && nextBare != null && !isPreposition(nextBare) && nextBare !in CONJUNCTIONS
                lastNounIrabCategory = IrabCategory.MANSOUB
                lastNounWasIndefinite = true
                lastNounWasDefinite = false
                continue
            }

            // 7e. Conjunction coordinated noun
            if (afterConjunction && lastNounIrabCategory != null) {
                val inheritedCat = lastNounIrabCategory
                val (state, full) = when (inheritedCat) {
                    IrabCategory.MARFOU -> {
                        val sign = marfouSignFor(reading, bare, detailedSigns)
                        (if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)") to
                            "اسم معطوف مرفوع وعلامة رفعه $sign"
                    }
                    IrabCategory.MANSOUB -> {
                        val sign = mansoubSignFor(reading, bare, detailedSigns)
                        (if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)") to
                            "اسم معطوف منصوب وعلامة نصبه $sign"
                    }
                    IrabCategory.MAJROUR -> {
                        val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, i, tokens)
                        val sign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                        (if (detailedSigns) "مجرور وعلامة جره $sign" else "مجرور ($sign)") to
                            "اسم معطوف مجرور وعلامة جره $sign"
                    }
                    else -> "تابع لما قبله" to "اسم معطوف تابع لما قبله في الإعراب"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "اسم معطوف",
                        state = state,
                        fullIrab = full,
                        category = inheritedCat
                    )
                )
                afterConjunction = false
                continue
            }

            // 7f. إنّ وأخواتها
            if (afterInna && !innaSubjectAssigned) {
                val sign = mansoubSignFor(reading, bare, detailedSigns)
                val state = if (isMabniNoun) {
                    "مبني في محل نصب اسم إنّ"
                } else {
                    if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)"
                }
                val full = if (isMabniNoun) {
                    "$displayNounPos مبني في محل نصب اسم إنّ"
                } else {
                    "اسم إنّ منصوب وعلامة نصبه $sign"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "اسم إنّ",
                        state = state,
                        fullIrab = full,
                        category = IrabCategory.MANSOUB
                    )
                )
                innaSubjectAssigned = true
                lastNounIrabCategory = IrabCategory.MANSOUB
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            if (afterInna && innaSubjectAssigned && !khabarAssigned) {
                val sign = marfouSignFor(reading, bare, detailedSigns)
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "خبر إنّ",
                        state = if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)",
                        fullIrab = "خبر إنّ مرفوع وعلامة رفعه $sign",
                        category = IrabCategory.MARFOU
                    )
                )
                khabarAssigned = true
                lastNounIrabCategory = IrabCategory.MARFOU
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            // 7g. كان وأخواتها
            if (inKanaClause && !kanaSubjectAssigned) {
                val sign = marfouSignFor(reading, bare, detailedSigns)
                val state = if (isMabniNoun) {
                    "مبني في محل رفع اسم كان"
                } else {
                    if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)"
                }
                val full = if (isMabniNoun) {
                    "$displayNounPos مبني في محل رفع اسم كان"
                } else {
                    "اسم كان مرفوع وعلامة رفعه $sign"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "اسم كان",
                        state = state,
                        fullIrab = full,
                        category = IrabCategory.MARFOU
                    )
                )
                kanaSubjectAssigned = true
                lastNounIrabCategory = IrabCategory.MARFOU
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            if (inKanaClause && kanaSubjectAssigned && !khabarAssigned) {
                val sign = mansoubSignFor(reading, bare, detailedSigns)
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "خبر كان",
                        state = if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)",
                        fullIrab = "خبر كان منصوب وعلامة نصبه $sign",
                        category = IrabCategory.MANSOUB
                    )
                )
                khabarAssigned = true
                lastNounIrabCategory = IrabCategory.MANSOUB
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            // 7h. Verbal sentence rules (نائب فاعل / فاعل / مفعول مطلق / مفعول به)
            if (isVerbalSentence && verbSeen) {
                val caseOrMood = explicitHaraka ?: reading?.caseOrMood
                val isExplicitlyAccusative = explicitHaraka == "منصوب"
                val isExplicitlyGenitive = explicitHaraka == "مجرور"
                val nounRoot = reading?.root ?: token.contextRoot

                // المفعول المطلق: مصدر منصوب يوافق جذر الفعل
                if (currentVerbRoot != null && nounRoot == currentVerbRoot &&
                    (isExplicitlyAccusative || token.originalWord.endsWith("\u064B") ||
                        subType?.contains("مصدر") == true) &&
                    subjectAssigned
                ) {
                    val sign = mansoubSignFor(reading, bare, detailedSigns)
                    results.add(
                        buildResult(
                            token = token,
                            reading = reading,
                            displayPos = displayNounPos,
                            role = "مفعول مطلق",
                            state = if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)",
                            fullIrab = "مفعول مطلق منصوب وعلامة نصبه $sign",
                            category = IrabCategory.MANSOUB
                        )
                    )
                    lastNounIrabCategory = IrabCategory.MANSOUB
                    lastNounWasIndefinite = !isDefiniteNoun
                    lastNounWasDefinite = isDefiniteNoun
                    continue
                }

                if (!subjectAssigned && !isExplicitlyAccusative && !isExplicitlyGenitive) {
                    val role = if (currentVerbIsPassive) "نائب فاعل" else "فاعل"
                    val sign = marfouSignFor(reading, bare, detailedSigns)
                    val state = if (isMabniNoun) {
                        "مبني في محل رفع $role"
                    } else {
                        if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)"
                    }
                    val full = if (isMabniNoun) {
                        "$displayNounPos مبني في محل رفع $role"
                    } else {
                        "$role مرفوع وعلامة رفعه $sign"
                    }
                    results.add(
                        buildResult(
                            token = token,
                            reading = reading,
                            displayPos = displayNounPos,
                            role = role,
                            state = state,
                            fullIrab = full,
                            category = IrabCategory.MARFOU
                        )
                    )
                    subjectAssigned = true
                    lastNounIrabCategory = IrabCategory.MARFOU
                    lastNounWasIndefinite = !isDefiniteNoun
                    lastNounWasDefinite = isDefiniteNoun
                    continue
                }

                if (lastNounWasIndefinite && isDefiniteNoun && isExplicitlyGenitive) {
                    val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, i, tokens)
                    val sign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                    results.add(
                        buildResult(
                            token = token,
                            reading = reading,
                            displayPos = displayNounPos,
                            role = "مضاف إليه",
                            state = if (detailedSigns) "مجرور وعلامة جره $sign" else "مجرور ($sign)",
                            fullIrab = "مضاف إليه مجرور وعلامة جره $sign",
                            category = IrabCategory.MAJROUR
                        )
                    )
                    lastNounIrabCategory = IrabCategory.MAJROUR
                    lastNounWasIndefinite = false
                    lastNounWasDefinite = true
                    continue
                }

                if (!objectAssigned && !currentVerbIsPassive && !isExplicitlyGenitive) {
                    val sign = mansoubSignFor(reading, bare, detailedSigns)
                    val state = if (isMabniNoun) {
                        "مبني في محل نصب مفعول به"
                    } else {
                        if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)"
                    }
                    val full = if (isMabniNoun) {
                        "$displayNounPos مبني في محل نصب مفعول به"
                    } else {
                        "مفعول به منصوب وعلامة نصبه $sign"
                    }
                    results.add(
                        buildResult(
                            token = token,
                            reading = reading,
                            displayPos = displayNounPos,
                            role = "مفعول به",
                            state = state,
                            fullIrab = full,
                            category = IrabCategory.MANSOUB
                        )
                    )
                    objectAssigned = true
                    lastNounIrabCategory = IrabCategory.MANSOUB
                    lastNounWasIndefinite = !isDefiniteNoun
                    lastNounWasDefinite = isDefiniteNoun
                    continue
                }

                val (role, state, full, cat) = resolveSecondaryNoun(
                    reading = reading,
                    bare = bare,
                    hasAlPrefix = hasAlPrefix,
                    index = i,
                    tokens = tokens,
                    explicitHaraka = caseOrMood,
                    isDefinite = isDefiniteNoun,
                    lastWasIndefinite = lastNounWasIndefinite,
                    lastWasDefinite = lastNounWasDefinite,
                    lastCat = lastNounIrabCategory,
                    detailedSigns = detailedSigns
                )
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = role,
                        state = state,
                        fullIrab = full,
                        category = cat
                    )
                )
                lastNounIrabCategory = cat
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            // 7i. Nominal sentence rules (المبتدأ والخبر)
            if (!mubtadaAssigned) {
                val sign = marfouSignFor(reading, bare, detailedSigns)
                val state = if (isMabniNoun) {
                    "مبني في محل رفع مبتدأ"
                } else {
                    if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)"
                }
                val full = if (isMabniNoun) {
                    "$displayNounPos مبني في محل رفع مبتدأ"
                } else {
                    "مبتدأ مرفوع وعلامة رفعه $sign"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "مبتدأ",
                        state = state,
                        fullIrab = full,
                        category = IrabCategory.MARFOU
                    )
                )
                mubtadaAssigned = true
                lastNounIrabCategory = IrabCategory.MARFOU
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            val hasNextToken = i + 1 < tokens.size
            if (!khabarAssigned && lastNounWasIndefinite && isDefiniteNoun &&
                (explicitHaraka == "مجرور" || (hasNextToken && !isPreposition(tokens[i + 1].bareWord)))
            ) {
                val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, i, tokens)
                val sign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "مضاف إليه",
                        state = if (detailedSigns) "مجرور وعلامة جره $sign" else "مجرور ($sign)",
                        fullIrab = "مضاف إليه مجرور وعلامة جره $sign",
                        category = IrabCategory.MAJROUR
                    )
                )
                lastNounIrabCategory = IrabCategory.MAJROUR
                lastNounWasIndefinite = false
                lastNounWasDefinite = true
                continue
            }

            if (!khabarAssigned && explicitHaraka != "مجرور") {
                val sign = marfouSignFor(reading, bare, detailedSigns)
                val state = if (isMabniNoun) {
                    "مبني في محل رفع خبر"
                } else {
                    if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)"
                }
                val full = if (isMabniNoun) {
                    "$displayNounPos مبني في محل رفع خبر"
                } else {
                    "خبر مرفوع وعلامة رفعه $sign"
                }
                results.add(
                    buildResult(
                        token = token,
                        reading = reading,
                        displayPos = displayNounPos,
                        role = "خبر",
                        state = state,
                        fullIrab = full,
                        category = IrabCategory.MARFOU
                    )
                )
                khabarAssigned = true
                lastNounIrabCategory = IrabCategory.MARFOU
                lastNounWasIndefinite = !isDefiniteNoun
                lastNounWasDefinite = isDefiniteNoun
                continue
            }

            val (role, state, full, cat) = resolveSecondaryNoun(
                reading = reading,
                bare = bare,
                hasAlPrefix = hasAlPrefix,
                index = i,
                tokens = tokens,
                explicitHaraka = explicitHaraka ?: reading?.caseOrMood,
                isDefinite = isDefiniteNoun,
                lastWasIndefinite = lastNounWasIndefinite,
                lastWasDefinite = lastNounWasDefinite,
                lastCat = lastNounIrabCategory,
                detailedSigns = detailedSigns
            )
            results.add(
                buildResult(
                    token = token,
                    reading = reading,
                    displayPos = displayNounPos,
                    role = role,
                    state = state,
                    fullIrab = full,
                    category = cat
                )
            )
            lastNounIrabCategory = cat
            lastNounWasIndefinite = !isDefiniteNoun
            lastNounWasDefinite = isDefiniteNoun
        }

        return SentenceIrabResult(
            sentence = raw.sentence,
            sentenceType = sentenceType,
            words = results,
            layer2Used = raw.layer2Used
        )
    }

    private fun resolveSecondaryNoun(
        reading: MorphReading?,
        bare: String,
        hasAlPrefix: Boolean,
        index: Int,
        tokens: List<RawTokenAnalysis>,
        explicitHaraka: String?,
        isDefinite: Boolean,
        lastWasIndefinite: Boolean,
        lastWasDefinite: Boolean,
        lastCat: IrabCategory?,
        detailedSigns: Boolean
    ): FourTuple {
        if (lastWasIndefinite && isDefinite) {
            val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, index, tokens)
            val sign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
            return FourTuple(
                "مضاف إليه",
                if (detailedSigns) "مجرور وعلامة جره $sign" else "مجرور ($sign)",
                "مضاف إليه مجرور وعلامة جره $sign",
                IrabCategory.MAJROUR
            )
        }

        if ((lastWasDefinite == isDefinite) && lastCat != null) {
            return when (lastCat) {
                IrabCategory.MARFOU -> {
                    val sign = marfouSignFor(reading, bare, detailedSigns)
                    FourTuple(
                        "نعت (صفة)",
                        if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)",
                        "نعت مرفوع وعلامة رفعه $sign",
                        IrabCategory.MARFOU
                    )
                }
                IrabCategory.MANSOUB -> {
                    val sign = mansoubSignFor(reading, bare, detailedSigns)
                    FourTuple(
                        "نعت (صفة)",
                        if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)",
                        "نعت منصوب وعلامة نصبه $sign",
                        IrabCategory.MANSOUB
                    )
                }
                IrabCategory.MAJROUR -> {
                    val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, index, tokens)
                    val sign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                    FourTuple(
                        "نعت (صفة)",
                        if (detailedSigns) "مجرور وعلامة جره $sign" else "مجرور ($sign)",
                        "نعت مجرور وعلامة جره $sign",
                        IrabCategory.MAJROUR
                    )
                }
                else -> FourTuple("اسم", "بحسب موقعه في الجملة", "اسم تابع لما قبله", IrabCategory.OTHER)
            }
        }

        return when (explicitHaraka) {
            "مرفوع" -> {
                val sign = marfouSignFor(reading, bare, detailedSigns)
                FourTuple(
                    "اسم مرفوع",
                    if (detailedSigns) "مرفوع وعلامة رفعه $sign" else "مرفوع ($sign)",
                    "اسم مرفوع وعلامة رفعه $sign",
                    IrabCategory.MARFOU
                )
            }
            "منصوب" -> {
                val sign = mansoubSignFor(reading, bare, detailedSigns)
                FourTuple(
                    "اسم منصوب",
                    if (detailedSigns) "منصوب وعلامة نصبه $sign" else "منصوب ($sign)",
                    "اسم منصوب وعلامة نصبه $sign",
                    IrabCategory.MANSOUB
                )
            }
            "مجرور" -> {
                val isDiptote = isDiptoteMajrour(bare, hasAlPrefix, index, tokens)
                val sign = majrourSignFor(reading, bare, isDiptote, detailedSigns)
                FourTuple(
                    "مضاف إليه",
                    if (detailedSigns) "مجرور وعلامة جره $sign" else "مجرور ($sign)",
                    "مضاف إليه مجرور وعلامة جره $sign",
                    IrabCategory.MAJROUR
                )
            }
            else -> FourTuple("اسم", "مكمل للجملة", "اسم واقع في سياق الجملة", IrabCategory.OTHER)
        }
    }

    private fun selectBestReading(
        token: RawTokenAnalysis,
        index: Int,
        allTokens: List<RawTokenAnalysis>
    ): MorphReading? {
        val bare = token.bareWord
        val readings = token.readings
        if (readings.isEmpty()) return token.primary

        if (bare in VOCATIVE_PARTICLES) {
            return readings.firstOrNull { it.partOfSpeech == "حرف" } ?: token.primary
        }

        if (bare in PREPOSITIONS) {
            return readings.firstOrNull { it.partOfSpeech == "حرف" && it.subType == "جر" }
                ?: readings.firstOrNull { it.partOfSpeech == "حرف" }
                ?: token.primary
        }

        if (bare in CONJUNCTIONS) {
            return readings.firstOrNull { it.partOfSpeech == "حرف" && it.subType == "عطف" }
                ?: readings.firstOrNull { it.partOfSpeech == "حرف" }
                ?: token.primary
        }

        if (isInnaSister(token.originalWord, bare) ||
            bare in SUBJUNCTIVE_PARTICLES ||
            bare in JUSSIVE_PARTICLES ||
            bare in NEGATION_PARTICLES
        ) {
            return readings.firstOrNull { it.partOfSpeech == "حرف" } ?: token.primary
        }

        if (bare in PRONOUNS || bare in DEMONSTRATIVES || bare in RELATIVES || bare in PLACE_ADVERBS || bare in TIME_ADVERBS) {
            return readings.firstOrNull { it.partOfSpeech == "اسم" } ?: token.primary
        }

        // Attached prepositions (بـ / كـ / لـ)
        if ((bare.startsWith("ب") || bare.startsWith("ل") || bare.startsWith("ك")) && bare.length > 3) {
            val prepReading = readings.firstOrNull { r ->
                r.partOfSpeech == "اسم" && r.proclitics.any { it.description?.contains("الجر") == true }
            }
            if (prepReading != null && token.explicitHaraka == "مجرور") {
                return prepReading
            }
            if (prepReading != null && (bare.startsWith("بال") || bare.startsWith("لل") || bare.startsWith("كال"))) {
                return prepReading
            }
        }

        val prevBare = allTokens.getOrNull(index - 1)?.bareWord ?: ""
        if (prevBare in PREPOSITIONS || prevBare in VOCATIVE_PARTICLES || prevBare in PLACE_ADVERBS) {
            return readings.firstOrNull { it.partOfSpeech == "اسم" } ?: token.primary
        }

        if (prevBare in SUBJUNCTIVE_PARTICLES || prevBare in JUSSIVE_PARTICLES) {
            return readings.firstOrNull { it.partOfSpeech == "فعل" && it.tense == "مضارع" }
                ?: readings.firstOrNull { it.partOfSpeech == "فعل" }
                ?: token.primary
        }

        // Sentence-initial verb vs noun disambiguation
        if (index == 0 || (index == 1 && prevBare in CONJUNCTIONS)) {
            val hasAl = bare.startsWith("ال") && bare.length > 3
            val hasTaMarbuta = bare.endsWith("ة")
            val hasExplicitTanweenOrGenitive = token.originalWord.any {
                it == '\u064B' || it == '\u064C' || it == '\u064D' || it == '\u0650'
            }
            // Prefer passive reading if explicit passive vocalization on first letter (ضمة)
            if (hasPassiveVocalization(token.originalWord)) {
                val passiveReading = readings.firstOrNull { it.partOfSpeech == "فعل" && it.voice == "مجهول" }
                if (passiveReading != null) return passiveReading
            }

            val verbReading = readings.firstOrNull { it.partOfSpeech == "فعل" }
            if (!hasAl && !hasTaMarbuta && !hasExplicitTanweenOrGenitive && verbReading != null) {
                val nextToken = allTokens.getOrNull(index + 1)
                val nextIsLikelySubject = nextToken != null &&
                    (nextToken.bareWord.startsWith("ال") ||
                        nextToken.explicitHaraka == "مرفوع" ||
                        nextToken.readings.any { it.partOfSpeech == "اسم" })

                if (token.primary?.partOfSpeech == "فعل") return token.primary

                if (token.explicitHaraka == "مرفوع") {
                    val presentVerb = readings.firstOrNull { it.partOfSpeech == "فعل" && it.tense == "مضارع" }
                    if (presentVerb != null && (bare.startsWith("ي") || bare.startsWith("ت") || bare.startsWith("ن") || bare.startsWith("أ"))) {
                        return presentVerb
                    }
                    return readings.firstOrNull { it.partOfSpeech == "اسم" } ?: token.primary
                }

                if (bare in PAST_TRANSITIVE_HINTS || bare in KANA_SISTERS_BARE ||
                    (verbReading.tense == "مضارع" && (bare.startsWith("ي") || bare.startsWith("ت"))) ||
                    (nextIsLikelySubject && verbReading.tense == "ماض" && allTokens.size >= 2)
                ) {
                    if (token.primary?.subType != "اسم علم" || bare == "ذهب" || bare == "كتب") {
                        return verbReading
                    }
                }
            }
        }

        return token.primary ?: readings.firstOrNull()
    }

    private fun isPassiveVerb(originalWord: String, reading: MorphReading?): Boolean {
        if (reading?.voice == "مجهول") return true
        return hasPassiveVocalization(originalWord)
    }

    private fun hasPassiveVocalization(word: String): Boolean {
        if (word.length < 4) return false
        val firstHasDamma = word.getOrNull(1) == '\u064F'
        val hasKasraInside = word.drop(2).dropLast(1).contains('\u0650')
        return firstHasDamma && hasKasraInside
    }

    private fun isFiveVerbForm(tense: String?, bare: String, afterParticle: Boolean): Boolean {
        if (tense != "مضارع") return false
        val startsLikePresent = bare.startsWith("ي") || bare.startsWith("ت")
        if (!startsLikePresent || bare.length < 5) return false
        if (!afterParticle && (bare.endsWith("ون") || bare.endsWith("ان") || bare.endsWith("ين"))) {
            return true
        }
        if (afterParticle && (bare.endsWith("وا") || bare.endsWith("ا") || bare.endsWith("ي"))) {
            return true
        }
        return false
    }

    private fun isDiptoteMajrour(
        bare: String,
        hasAlPrefix: Boolean,
        index: Int,
        tokens: List<RawTokenAnalysis>
    ): Boolean {
        if (hasAlPrefix) return false
        val nextBare = tokens.getOrNull(index + 1)?.bareWord
        val hasFollowingGenitive = nextBare != null && nextBare.startsWith("ال")
        if (hasFollowingGenitive) return false
        return bare in DIPTOTE_WORDS
    }

    private fun findFirstSignificantIndex(
        tokens: List<RawTokenAnalysis>,
        readings: List<MorphReading?>
    ): Int {
        for (i in tokens.indices) {
            val bare = tokens[i].bareWord
            if (bare in CONJUNCTIONS || readings[i]?.subType == "عطف") continue
            return i
        }
        return 0
    }

    private fun isPreposition(bare: String): Boolean = bare in PREPOSITIONS

    private fun isInnaSister(original: String, bare: String): Boolean {
        if (bare in setOf("إن", "ليت", "لعل", "كأن", "لكن")) return true
        if (bare == "أن" && original.contains('\u0651')) return true
        return false
    }

    private fun hasAttachedVerbSubject(reading: MorphReading?, bare: String): Boolean {
        if (reading == null) return false
        val p = reading.person
        if (p == "متكلم" || p == "مخاطب") return true
        if (reading.tense == "أمر") return true
        if (bare.endsWith("وا") || bare.endsWith("نا")) {
            return reading.enclitics.isNotEmpty() || reading.pronoun in setOf("هم", "نحن", "أنا", "أنتَ", "أنتِ")
        }
        return false
    }

    private fun marfouSignFor(reading: MorphReading?, bare: String, detailed: Boolean): String {
        return when {
            reading?.number == "مثنى" || (bare.endsWith("ان") && reading?.number != "مفرد") ->
                if (detailed) "الألف لأنه مثنى" else "الألف"
            reading?.number == "جمع" && reading.gender == "مذكر" && bare.endsWith("ون") ->
                if (detailed) "الواو لأنه جمع مذكر سالم" else "الواو"
            bare.endsWith("ى") || (bare.endsWith("ا") && bare.length > 2) ->
                if (detailed) "الضمة المقدرة للتعذر" else "الضمة المقدرة"
            else -> if (detailed) "الضمة الظاهرة" else "الضمة"
        }
    }

    private fun mansoubSignFor(reading: MorphReading?, bare: String, detailed: Boolean): String {
        return when {
            reading?.number == "مثنى" && bare.endsWith("ين") ->
                if (detailed) "الياء لأنه مثنى" else "الياء"
            reading?.number == "جمع" && reading.gender == "مذكر" && bare.endsWith("ين") ->
                if (detailed) "الياء لأنه جمع مذكر سالم" else "الياء"
            reading?.number == "جمع" && reading.gender == "مؤنث" && bare.endsWith("ات") ->
                if (detailed) "الكسرة نيابةً عن الفتحة لأنه جمع مؤنث سالم" else "الكسرة"
            else -> if (detailed) "الفتحة الظاهرة" else "الفتحة"
        }
    }

    private fun majrourSignFor(
        reading: MorphReading?,
        bare: String,
        isDiptote: Boolean,
        detailed: Boolean
    ): String {
        return when {
            isDiptote ->
                if (detailed) "الفتحة نيابةً عن الكسرة لأنه ممنوع من الصرف" else "الفتحة (ممنوع من الصرف)"
            reading?.number == "مثنى" && bare.endsWith("ين") ->
                if (detailed) "الياء لأنه مثنى" else "الياء"
            reading?.number == "جمع" && reading.gender == "مذكر" && bare.endsWith("ين") ->
                if (detailed) "الياء لأنه جمع مذكر سالم" else "الياء"
            else -> if (detailed) "الكسرة الظاهرة" else "الكسرة"
        }
    }

    private fun buildResult(
        token: RawTokenAnalysis,
        reading: MorphReading?,
        displayPos: String,
        role: String,
        state: String,
        fullIrab: String,
        category: IrabCategory
    ): WordIrabResult {
        return WordIrabResult(
            word = token.originalWord,
            diacritizedWord = reading?.diacritized?.takeIf { it.isNotBlank() } ?: token.originalWord,
            partOfSpeech = displayPos,
            syntacticRole = role,
            grammaticalState = state,
            fullIrab = fullIrab,
            category = category,
            lemma = reading?.lemma?.takeIf { it.isNotBlank() } ?: token.contextLemma ?: token.bareWord,
            root = reading?.root ?: token.contextRoot,
            stem = reading?.stem?.takeIf { it.isNotBlank() } ?: token.contextStem ?: token.bareWord,
            pattern = reading?.pattern,
            subType = reading?.subType,
            tense = reading?.tense,
            gender = reading?.gender,
            number = reading?.number,
            proclitics = reading?.proclitics ?: emptyList(),
            enclitics = reading?.enclitics ?: emptyList(),
            allReadings = token.readings
        )
    }

    private data class FiveTuple(
        val first: String,
        val second: String,
        val third: String,
        val fourth: String,
        val fifth: IrabCategory
    )

    private data class FourTuple(
        val first: String,
        val second: String,
        val third: String,
        val fourth: IrabCategory
    )
}
