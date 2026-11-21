(function(window) {
"use strict";
// src/normalize.ts
var ALL_ARABIC = "\u0671\u0621\u0622\u0623\u0625\u0624\u0626\u0627\u0628\u0629\u062A\u062B\u062C\u062D\u062E\u062F\u0630\u0631\u0632\u0633\u0634\u0635\u0636\u0637\u0638\u0639\u063A\u0641\u0642\u0643\u0644\u0645\u0646\u0647\u0648\u064A\u0649";
var ALL_DIACRITICS = "\u064B\u064C\u064D\u064E\u064F\u0650\u0651\u0652";
var ALL_DIACRITICS_RE = /[ًٌٍَُِّْ]/g;
var LAST_DIAC_RE = /[ًٌٍَُِْ]$/;
var ALL_HAMZA_RE = /[أإؤئ]/;
var HAMZA_TO_BARE_G = /[ؤأإئ]/g;
var DIACRITICS_SET = new Set(ALL_DIACRITICS);
var HAMZA_SET = new Set("\u0621\u0623\u0625\u0624\u0626");
var SOLAR = new Set("\u062A\u062B\u062F\u0630\u0631\u0632\u0633\u0634\u0635\u0636\u0637\u0638\u0644\u0646");
var DEFINIT_CLASSES = /* @__PURE__ */ new Set(["N1", "N2", "N3", "N5"]);
function isDiacritic(c) {
  return DIACRITICS_SET.has(c);
}
function isHamza(c) {
  return HAMZA_SET.has(c);
}
function isSolar(c) {
  return SOLAR.has(c);
}
function isDefinit(procClass) {
  return DEFINIT_CLASSES.has(procClass);
}
function getIsHamza(c) {
  return "\u0623\u0624\u0625\u0626".includes(c) ? "\u0621" : c;
}
function removeAllDiacritics(word) {
  return word.replace(ALL_DIACRITICS_RE, "");
}
function removeLastDiacritics(word) {
  return word.replace(LAST_DIAC_RE, "");
}
var TRAILING_DIACRITICS_RE = new RegExp("[" + ALL_DIACRITICS + "]+$");
function stripCaseEnding(word) {
  let m = TRAILING_DIACRITICS_RE.exec(word);
  if (m) {
    const kept = m[0].includes("\u0651") ? "\u0651" : "";
    return word.slice(0, m.index) + kept;
  }
  const last = word.slice(-1);
  if (last === "\u0627" || last === "\u0649") {
    m = TRAILING_DIACRITICS_RE.exec(word.slice(0, -1));
    if (m && m[0].includes("\u064B")) {
      const kept = m[0].includes("\u0651") ? "\u0651" : "";
      return word.slice(0, m.index) + kept + "\u064E" + last;
    }
  }
  return word;
}
function normalizeHamza(word) {
  return word.replace(HAMZA_TO_BARE_G, "\u0621");
}
function wordContainsHamza(word) {
  return ALL_HAMZA_RE.test(word);
}
function typeHamza(word) {
  if (wordContainsHamza(word)) {
    for (const c of word) {
      if ("\u0623\u0624\u0625\u0626".includes(c)) return c;
    }
  }
  return "";
}
function correctErreur(word) {
  if (word.length >= 3 && word[1] === "\u0651") {
    word = word[0] + word.slice(2);
  }
  word = word.replaceAll("\u0671", "\u0627");
  word = word.replaceAll("\u0627\u064F", "\u0627");
  word = word.replaceAll("\u0627\u064E\u0651", "\u064E\u0651\u0627");
  word = word.replace(/ىً$/, "\u064B\u0649");
  word = word.replace(/اً$/, "\u064B\u0627");
  word = word.replaceAll("\u0627\u064E", "\u0627");
  word = word.replaceAll("\u0627\u0650", "\u0627");
  word = word.replaceAll("\u0650\u0627", "\u0627");
  word = word.replaceAll("\u0622\u064E", "\u0622");
  return word;
}
function getDiacritizationStem(pattern, stem) {
  let result = "";
  let j = 0;
  for (const ch of pattern) {
    if (isDiacritic(ch)) {
      result += ch;
    } else {
      result += stem[j];
      j += 1;
    }
  }
  return result;
}
function addDiacBeforeString(encVoweled, result) {
  const head = encVoweled[0];
  if (head === "\u0648") return result.slice(0, -1) + "\u064F" + encVoweled;
  if (head === "\u064A") return result.slice(0, -1) + "\u0650" + encVoweled;
  if (head === "\u0627") return result.slice(0, -1) + "\u064E" + encVoweled;
  return result + encVoweled;
}
function wordFromRootAndPattern(root, diacLemma) {
  let out = "";
  let passe = false;
  let passe2 = false;
  for (const ch of diacLemma) {
    if (ch === "\u0641") {
      out += root[0];
    } else if (ch === "\u0639") {
      out += root[1];
    } else if (ch === "\u0644") {
      if (!passe) {
        out += root[2];
      } else if (!passe2) {
        out += root[3];
        if (root.length > 4) {
          passe2 = true;
        }
      } else {
        out += root[4];
      }
      if (root.length !== 3) {
        passe = true;
      }
    } else {
      out += ch;
    }
  }
  return correctHamza(out);
}
function isDiacPattern(stem, diac) {
  let i = 0;
  while (i < diac.length && i < stem.length) {
    const d = diac[i];
    if (d !== "\u0641" && d !== "\u0639" && d !== "\u0644" && d !== stem[i]) {
      return false;
    }
    i += 1;
  }
  return true;
}
function notCompatible(normalizedWord, voweledWord, segment = null, isTool = false) {
  const useHamza = segment !== null || isTool;
  let nor = normalizedWord;
  let vow = voweledWord;
  if (segment !== null || isTool) {
    const encUnv = segment === null ? "" : segment.enclitic.unvoweledform;
    if ((encUnv !== "" || isTool || nor[nor.length - 1] === "\u0650") && vow[vow.length - 1] === "\u0652" && isDiacritic(nor[nor.length - 1])) {
      nor = nor.slice(0, -1);
      vow = vow.slice(0, -1);
    }
  }
  const unor = removeAllDiacritics(nor);
  const uvow = removeAllDiacritics(vow);
  if (vow.endsWith("\u064E\u0627") && nor.endsWith("\u0627\u064B")) return true;
  if (unor.length !== uvow.length) return true;
  let inI = 0;
  let iv = 0;
  while (inI < nor.length && iv < vow.length) {
    const nc = nor[inI];
    const vc = vow[iv];
    let same = nc === vc || nc === "\u0627" && vc === "\u0649";
    if (useHamza && !same) {
      same = getIsHamza(nc) === getIsHamza(vc);
    }
    if (same) {
      inI += 1;
      iv += 1;
      continue;
    }
    if (!isDiacritic(nc) && !isDiacritic(vc)) return true;
    if (!isDiacritic(nc) && isDiacritic(vc)) {
      iv += 1;
      continue;
    }
    if (isDiacritic(nc)) return true;
  }
  return false;
}
function isNoRelatif(c) {
  return c !== "\u062F" && c !== "\u0630" && c !== "\u0631" && c !== "\u0632" && c !== "\u0648";
}
function startHamza(word) {
  let first;
  if (word.length > 1 && word[0] === "\u0621") {
    first = word[1] === "\u0650" ? "\u0625" : "\u0623";
  } else {
    first = word[0];
  }
  let wordF = first + word.slice(1);
  if (wordF.startsWith("\u0623\u064E\u0627")) {
    wordF = "\u0622" + wordF.slice(3);
  }
  return wordF;
}
function endHamza(word, pos) {
  if (pos > 1 && word[pos - 1] === "\u0650") return "\u0626";
  if (pos > 1 && word[pos - 1] === "\u064E") return "\u0623";
  if (pos > 3 && word[pos - 1] === "\u064F" && word[pos - 2] === "\u0651" && word[pos - 3] === "\u0648")
    return "\u0621";
  if (pos > 1 && word[pos - 1] === "\u064F") return "\u0624";
  return "\u0621";
}
function isNibraHamza(word, pos, length) {
  if (pos > 0 && word[pos - 1] === "\u0650") return true;
  if (pos > 0 && word[pos - 1] === "\u064A" || pos > 1 && word[pos - 1] === "\u0652" && word[pos - 2] === "\u064A") {
    return true;
  }
  if (pos > 1 && word[pos - 1] === "\u0652" && isNoRelatif(word[pos - 2]) && pos + 2 < length && (word[pos + 1] === "\u064E" || word[pos + 1] === "\u064B") && word[pos + 2] === "\u0627") {
    return true;
  }
  if (pos > 0 && (word[pos - 1] === "\u0627" || word[pos - 1] === "\u064A" || word[pos - 1] === "\u0648") && (pos + 1 < length && word[pos + 1] === "\u0650" || pos + 2 < length && word[pos + 2] === "\u0650" && word[pos + 1] === "\u0651")) {
    return true;
  }
  return pos > 0 && (word[pos - 1] === "\u0652" || word[pos - 1] === "\u064E" || word[pos - 1] === "\u064F") && (pos + 1 < length && word[pos + 1] === "\u0650" || pos + 2 < length && word[pos + 2] === "\u0650" && word[pos + 1] === "\u0651");
}
function isWawHamza(word, pos, length) {
  if (pos > 0 && (word[pos - 1] === "\u0627" || word[pos - 1] === "\u0652") && word[pos - 1] !== "\u064A" && word[pos - 1] !== "\u0648") {
    if (pos + 1 < length && word[pos + 1] === "\u064F") {
      return pos + 2 < length;
    }
    if (pos + 2 < length && word[pos + 2] === "\u064F" && word[pos + 1] === "\u0651") {
      return pos + 3 < length ? word[pos + 3] !== "\u0648" : false;
    }
  }
  if (pos > 0 && word[pos - 1] === "\u064E" && (pos + 1 < length && word[pos + 1] === "\u064F" || pos + 2 < length && word[pos + 2] === "\u064F" && word[pos + 1] === "\u0651")) {
    return true;
  }
  if (pos > 0 && word[pos - 1] === "\u064F" && (pos + 1 < length && word[pos + 1] !== "\u0650" || pos + 2 < length && word[pos + 2] !== "\u0650" && word[pos + 1] === "\u0651")) {
    return pos <= 2 || word[pos - 2] !== "\u0651" || word[pos - 3] !== "\u0648";
  }
  return false;
}
function isAlifHamza(word, pos, length) {
  if (pos > 0 && word[pos - 1] === "\u064E" && (pos + 1 < length && word[pos + 1] === "\u0652" || pos + 1 < length && word[pos + 1] === "\u064E" || pos + 2 < length && word[pos + 2] === "\u064E" && word[pos + 1] === "\u0651")) {
    return true;
  }
  if (pos > 1 && word[pos - 1] === "\u0652" && word[pos - 2] === "\u0648" && pos + 1 < length && word[pos + 1] === "\u064E") {
    return false;
  }
  if (pos > 0 && word[pos - 1] === "\u0652") {
    if (pos + 2 < length && word[pos + 1] === "\u064B" && word[pos + 2] === "\u0627") {
      return false;
    }
    if (pos + 4 < length && word[pos + 1] === "\u064E" && word[pos + 2] === "\u0627" && word[pos + 3] === "\u0646" && word[pos + 4] === "\u0650") {
      return false;
    }
    if (pos + 1 < length && word[pos + 1] === "\u064E" || pos + 2 < length && word[pos + 1] === "\u0651" && word[pos + 2] === "\u064E") {
      return true;
    }
  }
  return false;
}
function correctHamza(word) {
  word = word.replace(HAMZA_TO_BARE_G, "\u0621");
  word = startHamza(word);
  const wrdD = removeAllDiacritics(word);
  let length = word.length;
  if (word.startsWith("\u0627\u0621\u0652")) {
    word = "\u0627\u0626\u0652" + word.slice(3);
    length = word.length;
  }
  let out = "";
  let ip = 0;
  let i = 0;
  while (i < length) {
    const c = word[i];
    if (c === "\u0621") {
      if (ip + 1 === wrdD.length) {
        out += endHamza(word, i);
      } else if (isNibraHamza(word, i, length)) {
        out += "\u0626";
      } else if (isWawHamza(word, i, length)) {
        out += "\u0624";
      } else if (isAlifHamza(word, i, length)) {
        if (i + 2 < length && word[i + 1] === "\u064E" && word[i + 2] === "\u0627") {
          out += "\u0622";
          i += 2;
        } else if (i + 3 < length && word[i + 1] === "\u0651" && word[i + 2] === "\u064E" && word[i + 3] === "\u0627") {
          out += "\u0622\u064E\u0651";
          i += 3;
        } else if (i + 3 < length && word[i + 1] === "\u064E" && word[i + 2] === "\u0621" && word[i + 3] === "\u0652") {
          out += "\u0622";
          i += 3;
        } else {
          out += "\u0623";
        }
      } else {
        out += c;
      }
    } else {
      out += c;
    }
    if (!isDiacritic(word[i])) {
      ip += 1;
    }
    i += 1;
  }
  return out;
}
function correctStemHamza(s) {
  const length = s.length;
  let out = "";
  let i = 0;
  while (i < length) {
    const c = s[i];
    if (c === "\u0621" || c === "\u0623") {
      if (i + 1 < length && s[i + 1] === "\u0627") {
        if (i - 1 >= 0 && s[i - 1] === "\u0627") {
          out += c;
        } else {
          out += "\u0622";
          i += 1;
        }
      } else {
        out += c;
      }
    } else {
      out += c;
    }
    i += 1;
  }
  return out;
}

// src/clitics.ts
var MAX_PROCLITIC = 5;
var MAX_ENCLITIC = 5;
var Clitic = class _Clitic {
  unvoweledform;
  voweledform;
  desc;
  classe;
  constructor(unvoweledform, voweledform, desc, classe) {
    this.unvoweledform = unvoweledform;
    this.voweledform = voweledform;
    this.desc = desc;
    this.classe = classe;
  }
  static fromDict(d) {
    return new _Clitic(d.unvoweledform, d.voweledform, d.desc || "", d.classe);
  }
};
var Segment = class {
  proclitic;
  stem;
  enclitic;
  constructor(proclitic, stem, enclitic) {
    this.proclitic = proclitic;
    this.stem = stem;
    this.enclitic = enclitic;
  }
};
function proclitics(lex, token) {
  const out = [];
  const size = token.length;
  let ip = 0;
  while (ip < size && ip <= MAX_PROCLITIC) {
    for (const d of lex.proclitics(token.slice(0, ip))) {
      out.push(Clitic.fromDict(d));
    }
    ip += 1;
  }
  return out;
}
function enclitics(lex, token) {
  const out = [];
  const size = token.length;
  let ip = 0;
  while (ip < size && ip <= MAX_ENCLITIC) {
    for (const d of lex.enclitics(token.slice(size - ip, size))) {
      out.push(Clitic.fromDict(d));
    }
    ip += 1;
  }
  return out;
}
function getAlternatives(stem, st, valid, validSuf) {
  if (st === "\u062A") return valid ? stem.slice(0, -1) + "\u0629" : "";
  if (st === "\u0622") return valid ? stem.slice(0, -1) + "\u0623\u0649" : "";
  if (st === "\u0627") return valid ? stem.slice(0, -1) + "\u0649" : "";
  if (st === "\u0648") return valid ? stem + "\u0627" : "";
  if (st === "\u0626" || st === "\u0624") return validSuf ? stem.slice(0, -1) + "\u0621" : "";
  return "";
}
function isValidSegment(seg) {
  const proc = seg.proclitic.classe;
  const enc = seg.enclitic.classe;
  return (!enc.startsWith("V") || !proc.startsWith("N")) && (!proc.startsWith("V") || !enc.startsWith("N")) && (!isDefinit(proc) || seg.enclitic.unvoweledform === "");
}
function getListsSegment(lex, unvoweledWord) {
  const result = [];
  const procs = proclitics(lex, unvoweledWord);
  const encs = enclitics(lex, unvoweledWord);
  let valid = true;
  const wlen = unvoweledWord.length;
  for (const p of procs) {
    for (const s of encs) {
      if (!valid) break;
      if (wlen - s.unvoweledform.length - p.unvoweledform.length >= 0) {
        const alternatives = /* @__PURE__ */ new Set();
        let stem = unvoweledWord.slice(p.unvoweledform.length, wlen - s.unvoweledform.length);
        alternatives.add(stem);
        if ((p.classe === "N1" || p.classe === "N2" || p.classe === "N3" || p.classe === "N5") && p.unvoweledform.endsWith("\u0644") && !stem.startsWith("\u0644")) {
          alternatives.add("\u0644" + stem);
        }
        if (s.unvoweledform === "\u064A" && !stem.startsWith("\u064A")) {
          alternatives.add(stem + "\u064A");
        }
        if ((p.classe === "N4" || p.classe === "C3") && p.unvoweledform.endsWith("\u0644") && stem.startsWith("\u0644")) {
          alternatives.add(
            getAlternatives(stem, stem[stem.length - 1], s.unvoweledform.length !== 0, s.unvoweledform.length > 0)
          );
          stem = "\u0627" + stem;
        }
        alternatives.add(stem);
        if (p.unvoweledform === "\u0623" || p.unvoweledform === "\u0628") {
          alternatives.add("\u0627" + stem);
        }
        let stem1 = "";
        if (stem.length > 1) {
          stem1 = getAlternatives(
            stem,
            stem[stem.length - 1],
            s.unvoweledform.length !== 0,
            s.unvoweledform.length > 0
          );
        }
        if (stem1 !== "") {
          alternatives.add(stem1);
        }
        const stem3 = stem1.replaceAll("\u0622", "\u0621\u0627");
        if (stem3 !== stem1) {
          alternatives.add(stem3);
        }
        const stem2 = stem.replaceAll("\u0622", "\u0621\u0627");
        if (stem2 !== stem) {
          alternatives.add(stem2);
        }
        for (const sch of alternatives) {
          const seg = new Segment(p, sch, s);
          if (isValidSegment(seg)) {
            result.push(seg);
          }
        }
      } else {
        valid = false;
      }
    }
  }
  return result;
}

// src/freq.ts
function decompose(x) {
  const buf = new ArrayBuffer(8);
  const dv = new DataView(buf);
  dv.setFloat64(0, x);
  const hi = dv.getUint32(0);
  const lo = dv.getUint32(4);
  const rawExp = hi >>> 20 & 2047;
  const hiFrac = BigInt(hi & 1048575);
  let mant = hiFrac << 32n | BigInt(lo >>> 0);
  let e;
  if (rawExp === 0) {
    e = -1074;
  } else {
    mant |= 1n << 52n;
    e = rawExp - 1075;
  }
  return { m: mant, e };
}
function toFixedHalfEven(value, digits) {
  if (!Number.isFinite(value)) return String(value);
  const neg = value < 0;
  const { m, e } = decompose(Math.abs(value));
  const P = 10n ** BigInt(digits);
  let scaled;
  if (m === 0n) {
    scaled = 0n;
  } else if (e >= 0) {
    scaled = m * P * (1n << BigInt(e));
  } else {
    const denom = 1n << BigInt(-e);
    const num = m * P;
    let q = num / denom;
    const twiceRem = num % denom * 2n;
    if (twiceRem > denom) {
      q += 1n;
    } else if (twiceRem === denom && q % 2n === 1n) {
      q += 1n;
    }
    scaled = q;
  }
  const intPart = scaled / P;
  const frac = (scaled % P).toString().padStart(digits, "0");
  const s = `${intPart.toString()}.${frac}`;
  return neg && scaled !== 0n ? `-${s}` : s;
}
function fmtFreq(value) {
  return toFixedHalfEven(value, 10);
}

// src/lexicon.ts
var TABLES = {
  // clitics
  proclitics: "DATA.Clitics.Proclitics.map",
  enclitics: "DATA.Clitics.Enclitics.map",
  // exceptional
  exceptional: "DATA.Exceptional.map",
  // proper nouns
  propernoun_unvoweled: "DATA.NonDerived.Propernoun.Unvoweled.map",
  propernoun_voweled: "DATA.NonDerived.Propernoun.Voweled.list",
  propernoun_pos: "DATA.NonDerived.Propernoun.PartOfSpeech.list",
  propernoun_casemood: "DATA.NonDerived.Propernoun.CaseOrMood.list",
  // toolwords
  toolwords_unvoweled: "DATA.NonDerived.Toolwords.Unvoweled.map",
  toolwords_voweled: "DATA.NonDerived.Toolwords.Voweled.list",
  toolwords_pos: "DATA.NonDerived.Toolwords.PartOfSpeech.list",
  // roots
  root_freq: "DATA.Root.map"
};
var DERIVED_TABLES = {
  unvoweled_stem: "DATA.Derived.{cat}.Patterns.Stems.Unvoweled.map",
  voweled_diac_stem: "DATA.Derived.{cat}.Patterns.Stems.Voweled.Diac.map",
  voweled_canonic_stem: "DATA.Derived.{cat}.Patterns.Stems.Voweled.Canonic.map",
  voweled_diac_lemma: "DATA.Derived.{cat}.Patterns.Lemmas.Voweled.Diac.map",
  voweled_canonic_lemma: "DATA.Derived.{cat}.Patterns.Lemmas.Voweled.Canonic.map",
  roots_tri: "DATA.Derived.{cat}.Roots.Trilateral.map",
  roots_quad: "DATA.Derived.{cat}.Roots.Quadriliteral.map",
  roots_id_tri: "DATA.Derived.{cat}.Roots.id.Trilateral.map",
  roots_id_quad: "DATA.Derived.{cat}.Roots.id.Quadriliteral.map",
  formulas: "DATA.Derived.{cat}.Formulas.map",
  pos: "DATA.Derived.{cat}.PartOfSpeech.list",
  casemood: "DATA.Derived.{cat}.CaseOrMood.list"
};
var FEATURE_TABLES = {
  Main: "DATA.Derived.{cat}.PartOfSpeech.Main.list",
  Type: "DATA.Derived.{cat}.PartOfSpeech.Type.list",
  NbRoot: "DATA.Derived.{cat}.PartOfSpeech.NbRoot.list",
  // verbs
  Augmented: "DATA.Derived.Verbs.PartOfSpeech.Augmented.list",
  Emphasized: "DATA.Derived.Verbs.PartOfSpeech.Emphasized.list",
  Person: "DATA.Derived.Verbs.PartOfSpeech.Person.list",
  Person2: "DATA.Derived.Verbs.PartOfSpeech.Person2.list",
  Transitivity: "DATA.Derived.Verbs.PartOfSpeech.Transitivity.list",
  Voice: "DATA.Derived.Verbs.PartOfSpeech.Voice.list",
  // nouns
  Definit: "DATA.Derived.Nouns.PartOfSpeech.Definit.list",
  Gender: "DATA.Derived.Nouns.PartOfSpeech.Gender.list",
  Number: "DATA.Derived.Nouns.PartOfSpeech.Number.list"
};
var DATA_DIR = "";
var mapCache = /* @__PURE__ */ new Map();
var listCache = /* @__PURE__ */ new Map();
var rawTextCache = /* @__PURE__ */ new Map();
function provideRawText(gzFilename, text) {
  rawTextCache.set(gzFilename, text);
}
function hasRawText(gzFilename) {
  return rawTextCache.has(gzFilename);
}
function consumeText(gzFilename, encoding) {
  const provided = rawTextCache.get(gzFilename);
  if (provided !== void 0) {
    rawTextCache.delete(gzFilename);
    return provided;
  }
  if (typeof globalThis.__farahidiLoadRawText === "function") {
    return globalThis.__farahidiLoadRawText(gzFilename);
  }
  throw new Error("Missing preloaded table: " + gzFilename);
}
function readLines(filename) {
  return consumeText(filename + ".jsonl.gz", "utf-8").split("\n");
}
function readGzipText(filenameWithExt) {
  return consumeText(filenameWithExt, "latin1");
}
function loadMap(filename) {
  const hit = mapCache.get(filename);
  if (hit) return hit;
  const out = /* @__PURE__ */ new Map();
  for (const line of readLines(filename)) {
    if (!line.trim()) continue;
    const rec = JSON.parse(line);
    out.set(rec.key, rec.value);
  }
  mapCache.set(filename, out);
  return out;
}
function loadList(filename) {
  const hit = listCache.get(filename);
  if (hit) return hit;
  const out = [];
  for (const line of readLines(filename)) {
    if (!line.trim()) continue;
    out.push(JSON.parse(line));
  }
  listCache.set(filename, out);
  return out;
}
var Lexicon = class {
  // --- generic map/list resolution -------------------------------------
  map(logical) {
    return loadMap(TABLES[logical]);
  }
  list(logical) {
    return loadList(TABLES[logical]);
  }
  derivedMap(cat, logical) {
    return loadMap(DERIVED_TABLES[logical].replace("{cat}", cat));
  }
  derivedList(cat, logical) {
    return loadList(DERIVED_TABLES[logical].replace("{cat}", cat));
  }
  // --- clitics ----------------------------------------------------------
  proclitics(surface) {
    return this.map("proclitics").get(surface) ?? [];
  }
  enclitics(surface) {
    return this.map("enclitics").get(surface) ?? [];
  }
  // --- derived: patterns / roots / formulas -----------------------------
  unvoweledStems(cat, length) {
    return this.derivedMap(cat, "unvoweled_stem").get(length) ?? [];
  }
  voweledDiacStem(cat, length, index) {
    const lst = this.derivedMap(cat, "voweled_diac_stem").get(length);
    return lst && index > 0 && index <= lst.length ? lst[index - 1] : null;
  }
  voweledCanonicStem(cat, length, index) {
    const lst = this.derivedMap(cat, "voweled_canonic_stem").get(length);
    return lst && index > 0 && index <= lst.length ? lst[index - 1] : null;
  }
  voweledDiacLemma(cat, length, index) {
    const lst = this.derivedMap(cat, "voweled_diac_lemma").get(length);
    return lst && index > 0 && index <= lst.length ? lst[index - 1] : null;
  }
  voweledCanonicLemma(cat, length, index) {
    const lst = this.derivedMap(cat, "voweled_canonic_lemma").get(length);
    return lst && index > 0 && index <= lst.length ? lst[index - 1] : null;
  }
  formulas(cat, length) {
    return this.derivedMap(cat, "formulas").get(length) ?? [];
  }
  formula(cat, length, formulaId) {
    const lst = this.formulas(cat, length);
    return formulaId > 0 && formulaId <= lst.length ? lst[formulaId - 1] : null;
  }
  containsRoot(cat, root) {
    const key = root.length === 3 ? "roots_id_tri" : "roots_id_quad";
    return this.derivedMap(cat, key).has(root);
  }
  /** The Root record (with len1..len12 formula-id slots) for `root`. */
  rootEntity(cat, root) {
    let idxMap;
    let charMap;
    if (root.length === 3) {
      idxMap = this.derivedMap(cat, "roots_id_tri");
      charMap = this.derivedMap(cat, "roots_tri");
    } else {
      idxMap = this.derivedMap(cat, "roots_id_quad");
      charMap = this.derivedMap(cat, "roots_quad");
    }
    if (!idxMap.has(root)) return null;
    const bucket = charMap.get(root[0]);
    if (bucket == null) return null;
    const idx = idxMap.get(root);
    return idx >= 0 && idx < bucket.length ? bucket[idx] : null;
  }
  // --- part of speech & features ---------------------------------------
  pos(cat, posId) {
    const lst = this.derivedList(cat, "pos");
    return posId > 0 && posId <= lst.length ? lst[posId - 1] : null;
  }
  casemood(cat, casemoodId) {
    const lst = this.derivedList(cat, "casemood");
    return casemoodId > 0 && casemoodId <= lst.length ? lst[casemoodId - 1] : null;
  }
  feature(cat, feature, index) {
    const lst = loadList(FEATURE_TABLES[feature].replace("{cat}", cat));
    return index > 0 && index <= lst.length ? lst[index - 1] : null;
  }
};
var CATEGORIES = ["Verbs", "Nouns"];
function layer1DataFiles() {
  const files = /* @__PURE__ */ new Set();
  for (const base of Object.values(TABLES)) files.add(base + ".jsonl.gz");
  for (const cat of CATEGORIES)
    for (const tpl of Object.values(DERIVED_TABLES)) files.add(tpl.replace("{cat}", cat) + ".jsonl.gz");
  for (const cat of CATEGORIES)
    for (const tpl of Object.values(FEATURE_TABLES)) files.add(tpl.replace("{cat}", cat) + ".jsonl.gz");
  return [...files];
}
function layer2DataFiles() {
  return [
    "DATA.MSA-LEMMA.ALL-train.map.jsonl.gz",
    "DATA.MSA.SHA.STEM.map.jsonl.gz",
    "DATA.MSA.SHA.ROOT.map.jsonl.gz",
    "DATA.MSA.ALL.TRAIN.141809.lm.gz"
  ];
}

// src/morphology.ts
var TENSES = /* @__PURE__ */ new Set(["\u0645\u0627\u0636", "\u0645\u0636\u0627\u0631\u0639", "\u0623\u0645\u0631"]);
var GENDERS = /* @__PURE__ */ new Set(["\u0645\u0630\u0643\u0631", "\u0645\u0624\u0646\u062B", "\u0645\u0630\u0643\u0631 \u0623\u0648 \u0645\u0624\u0646\u062B"]);
var NUMBERS = /* @__PURE__ */ new Set(["\u0645\u0641\u0631\u062F", "\u0645\u062B\u0646\u0649", "\u062C\u0645\u0639"]);
var PERSONS = { \u0627\u0644\u063A\u0627\u0626\u0628: "\u063A\u0627\u0626\u0628", \u0627\u0644\u0645\u062A\u0643\u0644\u0645: "\u0645\u062A\u0643\u0644\u0645", \u0627\u0644\u0645\u062E\u0627\u0637\u0628: "\u0645\u062E\u0627\u0637\u0628" };
var SINGLE_SLOT = {
  "\u0627\u0633\u0645 \u0627\u0644\u062C\u0644\u0627\u0644\u0629": ["\u0627\u0633\u0645", "\u0627\u0633\u0645 \u0627\u0644\u062C\u0644\u0627\u0644\u0629"],
  "\u062C\u0627\u0631 \u0648\u0645\u062C\u0631\u0648\u0631": ["\u062D\u0631\u0641", "\u062C\u0627\u0631 \u0648\u0645\u062C\u0631\u0648\u0631"]
};
function parsePosLabel(label) {
  const slots = label.split("|");
  const out = {
    pos: slots[0],
    type: null,
    tense: null,
    gender: null,
    number: null,
    person: null
  };
  if (slots.length === 1 && slots[0] in SINGLE_SLOT) {
    [out.pos, out.type] = SINGLE_SLOT[slots[0]];
  }
  for (const slot of slots.slice(1)) {
    if (!slot || slot === "-") continue;
    if (TENSES.has(slot) && out.tense === null) out.tense = slot;
    else if (GENDERS.has(slot) && out.gender === null) out.gender = slot;
    else if (NUMBERS.has(slot) && out.number === null) out.number = slot;
    else if (slot in PERSONS && out.person === null) out.person = PERSONS[slot];
    else if (out.type === null) out.type = slot;
  }
  return out;
}
function morphKey(m) {
  const affixes = (xs) => xs.map((a) => `${a.form}${a.description ?? ""}`).join("");
  return [
    m.diacritized,
    m.lemma,
    m.root ?? "",
    m.stem,
    m.pattern ?? "",
    m.lemmaPattern ?? "",
    m.pos,
    m.type ?? "",
    m.tense ?? "",
    m.gender ?? "",
    m.number ?? "",
    m.person ?? "",
    m.pronoun ?? "",
    m.voice ?? "",
    m.transitivity ?? "",
    m.augmentation ?? "",
    m.emphatic === null ? "" : String(m.emphatic),
    affixes(m.proclitics),
    affixes(m.prefixes),
    affixes(m.suffixes),
    affixes(m.enclitics)
  ].join("");
}
function collapse(raws) {
  const groups = /* @__PURE__ */ new Map();
  for (const raw of raws) {
    const key = morphKey(raw.morph);
    let group = groups.get(key);
    if (!group) {
      group = { morph: raw.morph, inflections: /* @__PURE__ */ new Map() };
      groups.set(key, group);
    }
    const infl = raw.inflection;
    const ikey = [infl.diacritized, infl.case ?? "", infl.mood ?? "", infl.state ?? ""].join(
      ""
    );
    const prev = group.inflections.get(ikey);
    if (prev === void 0 || prev.score < infl.score) group.inflections.set(ikey, infl);
  }
  const out = [];
  for (const { morph, inflections } of groups.values()) {
    const sorted = [...inflections.values()].sort((a, b) => b.score - a.score);
    out.push({ ...morph, score: sorted[0].score, inflections: sorted });
  }
  out.sort((a, b) => b.score - a.score);
  return out;
}

// src/pos.ts
var VERB_FEATURES = [
  ["Main", "main"],
  ["Type", "type"],
  ["Augmented", "augmented"],
  ["Emphasized", "emphasized"],
  ["NbRoot", "nbroot"],
  ["Person", "person"],
  ["Person2", "person2"],
  ["Transitivity", "transitivity"],
  ["Voice", "voice"]
];
var NOUN_FEATURES = [
  ["Main", "main"],
  ["Type", "type"],
  ["Definit", "definit"],
  ["Gender", "gender"],
  ["NbRoot", "nbroot"],
  ["Number", "number"]
];
function decode(lex, cat, pos, features) {
  const out = {};
  for (const [feature, field] of features) {
    const code = pos[field];
    const ve = code != null ? lex.feature(cat, feature, Number(code)) : null;
    out[feature] = ve ? ve.valAR : "";
  }
  return out;
}
function decodeVerb(lex, posId) {
  const pos = lex.pos("Verbs", posId);
  const d = decode(lex, "Verbs", pos, VERB_FEATURES);
  d.freq = pos.freq;
  d.partofspeech = [
    d.Main,
    d.Type,
    d.Emphasized,
    d.Voice,
    d.NbRoot,
    d.Augmented,
    d.Person,
    d.Person2,
    d.Transitivity
  ].join("|");
  return d;
}
function decodeNoun(lex, posId) {
  const pos = lex.pos("Nouns", posId);
  const d = decode(lex, "Nouns", pos, NOUN_FEATURES);
  d.freq = pos.freq;
  return d;
}

// src/vowelize.ts
var V4_END_RE = /[ًٌٍَُِْ]$/;
function vowelize(segment, pattern) {
  const procClass = segment.proclitic.classe;
  const enc = segment.enclitic;
  const stem = segment.stem;
  if (enc.classe === "V4") {
    pattern = pattern.replace(V4_END_RE, "\u064F");
  }
  let result = getDiacritizationStem(pattern, stem);
  result = result[0] + (isDefinit(procClass) && isSolar(stem[0]) ? "\u0651" : "") + result.slice(1);
  const soukoun = isDefinit(procClass) && !isSolar(stem[0]) ? "\u0652" : "";
  if (enc.voweledform !== "") {
    result = result.replaceAll("\u0629", "\u062A");
  }
  if ((result.endsWith("\u064F\u0648\u0627") || result.endsWith("\u0648\u0652\u0627")) && enc.unvoweledform !== "") {
    result = result.slice(0, -1);
  }
  const endsI = result.endsWith("\u064A") || result.endsWith("\u064A\u0652") || result.endsWith("\u0650");
  if (enc.unvoweledform === "\u0647") enc.voweledform = endsI ? "\u0647\u0650" : "\u0647\u064F";
  if (enc.unvoweledform === "\u0647\u0645\u0627") enc.voweledform = endsI ? "\u0647\u0650\u0645\u064E\u0627" : "\u0647\u064F\u0645\u064E\u0627";
  if (enc.unvoweledform === "\u0647\u0645") enc.voweledform = endsI ? "\u0647\u0650\u0645\u0652" : "\u0647\u064F\u0645\u0652";
  if (enc.unvoweledform === "\u0647\u0646") enc.voweledform = endsI ? "\u0647\u0650\u0646\u064E\u0651" : "\u0647\u064F\u0646\u064E\u0651";
  const last = result[result.length - 1];
  if ((last === "\u064F" || last === "\u064E") && enc.unvoweledform === "\u064A") {
    result = result.slice(0, -1) + "\u0650";
  }
  const encVoweled = enc.voweledform;
  let res;
  if (encVoweled.length > 0) {
    result = result.replaceAll("\u0649", "\u0627");
    if ((!result.endsWith("\u064A\u0652") || enc.classe !== "N1") && (result.endsWith("\u0652") || encVoweled.length === 1 || !isDiacritic(encVoweled[1]))) {
      res = addDiacBeforeString(encVoweled, result);
    } else {
      res = result + encVoweled;
    }
  } else {
    res = result;
  }
  if (res !== "") {
    res = correctHamza(res);
  }
  const head = segment.proclitic.unvoweledform === "\u0623" && res.length > 3 && res[0] === "\u0627" && res[2] === "\u0652" ? res.slice(1) : res;
  let resultat0 = segment.proclitic.voweledform + soukoun + head;
  if (resultat0.endsWith("\u064A\u0652\u064A") && enc.classe === "N1") {
    resultat0 = resultat0.replace(/يْي$/, "\u064A\u0651\u064E");
  }
  return [resultat0, result];
}

// src/analyzer.ts
var NUMBERED = "123456789";
function opt(value) {
  return value == null || value === "" || value === "-" || value === "#" ? null : value;
}
function cliticAffixes(clitic) {
  return clitic.voweledform ? [{ form: clitic.voweledform, description: opt(clitic.desc) }] : [];
}
function makeAnalysis(voweled, proclitic, stem, pos, diacPatternStem, patternStem, lemma, patternLemma, root, casemood, enclitic, priority, extras) {
  const dps = removeLastDiacritics(diacPatternStem);
  if (root !== "#" && root !== "-" && dps !== "-") {
    stem = wordFromRootAndPattern(root, dps);
  }
  const cm = opt(casemood);
  const f = extras.features;
  let pattern = opt(patternStem);
  let diacritized = voweled;
  let stemM = stem;
  if (cm !== null) {
    diacritized = stripCaseEnding(voweled);
    stemM = stripCaseEnding(stem);
    if (pattern !== null) {
      pattern = stripCaseEnding(pattern.endsWith("\u064B\u0627") ? pattern.slice(0, -1) : pattern);
    }
  }
  const morph = {
    diacritized,
    lemma,
    root: opt(root),
    stem: stemM,
    pattern,
    lemmaPattern: opt(patternLemma),
    pos: f.pos,
    type: f.type ?? null,
    tense: f.tense ?? null,
    gender: f.gender ?? null,
    number: f.number ?? null,
    person: f.person ?? null,
    pronoun: f.pronoun ?? null,
    voice: f.voice ?? null,
    transitivity: f.transitivity ?? null,
    augmentation: f.augmentation ?? null,
    emphatic: extras.emphatic ?? null,
    proclitics: extras.proclitics ?? [],
    prefixes: extras.prefixes ?? [],
    suffixes: extras.suffixes ?? [],
    enclitics: extras.enclitics ?? [],
    score: 0,
    inflections: []
  };
  const inflection = {
    diacritized: voweled,
    case: f.pos !== "\u0641\u0639\u0644" ? cm : null,
    mood: f.pos === "\u0641\u0639\u0644" ? cm : null,
    state: extras.state ?? null,
    score: Number(priority)
  };
  return {
    voweledWord: voweled,
    proclitic,
    stem,
    partOfSpeech: pos,
    diacPatternStem: dps,
    patternStem,
    lemma,
    patternLemma,
    root,
    caseOrMood: casemood,
    enclitic,
    priority,
    morph,
    inflection
  };
}
var Analyzer = class _Analyzer {
  lex;
  verbCache;
  nounCache;
  constructor() {
    this.lex = new Lexicon();
    this.verbCache = /* @__PURE__ */ new Map();
    this.nounCache = /* @__PURE__ */ new Map();
  }
  // ------------------------------------------------------------------ API
  /**
   * All morphological analyses of `word`, best `score` first.
   *
   * Readings that differ only in their إعراب ending are returned once, with the
   * endings nested under {@link Analysis.inflections}.
   */
  analyze(word) {
    return collapse(this._processToken(word));
  }
  /**
   * Port of `processToken` — one raw record per *(reading × ending)*, in
   * pipeline order (unsorted), exactly as the Java reference.
   * @internal
   */
  _processToken(word) {
    const lex = this.lex;
    const unvoweled = removeAllDiacritics(word);
    let normalized = correctErreur(word);
    const exc = this.analyzeExceptional(normalized, unvoweled);
    if (exc.length) return exc;
    const segments = getListsSegment(lex, unvoweled);
    const results = [];
    for (const seg of segments) {
      results.push(...this.analyzeProperNoun(normalized, unvoweled, seg));
      results.push(
        ...this.filtreHamza(normalized, this.analyzeToolwords(normalized, unvoweled, seg))
      );
      results.push(...this.filtreHamza(normalized, this.analyzeNominal(normalized, seg)));
      results.push(...this.filtreHamza(normalized, this.analyzeVerbal(normalized, seg)));
    }
    if (results.length === 0 && normalized.endsWith("\u064B\u0627") && !normalized.endsWith("\u064B\u0651\u0627")) {
      normalized = normalized.replace(/ًا$/, "\u064B\u0651\u0627");
      for (const seg of segments) {
        results.push(...this.analyzeProperNoun(normalized, unvoweled, seg));
        results.push(...this.analyzeNominal(normalized, seg));
      }
    }
    return results;
  }
  // -------------------------------------------------------------- helpers
  filtreHamza(normalizedWord, results) {
    const hamza = typeHamza(normalizedWord);
    if (hamza === "") return results;
    return results.filter((r) => r.stem.includes(hamza));
  }
  // ----------------------------------------------------------- exceptional
  analyzeExceptional(normalizedWord, unvoweledWord) {
    const ew = this.lex.map("exceptional").get(unvoweledWord);
    if (ew == null) return [];
    const proc = opt(ew.proclitic);
    const enc = opt(ew.enclitic);
    return [
      makeAnalysis(
        ew.voweledform,
        ew.proclitic,
        ew.stem,
        ew.pos,
        "-",
        "-",
        "\u0627\u0644\u0644\u0647",
        "-",
        "-",
        "-",
        ew.enclitic,
        "1",
        {
          features: parsePosLabel(ew.pos),
          proclitics: proc ? [{ form: proc, description: null }] : [],
          enclitics: enc ? [{ form: enc, description: null }] : []
        }
      )
    ];
  }
  // ------------------------------------------------------------- toolwords
  analyzeToolwords(normalizedWord, unvoweledWord, seg) {
    const lex = this.lex;
    const stem = seg.stem;
    const unv = lex.map("toolwords_unvoweled").get(stem);
    if (unv == null) return [];
    const proc = seg.proclitic;
    const enc = seg.enclitic;
    const voweledList = lex.list("toolwords_voweled");
    const posList = lex.list("toolwords_pos");
    const out = [];
    for (const sid of unv.split(" ")) {
      const tw = voweledList[Number(sid) - 1];
      const procClass = tw.procClass || "";
      const encClass = tw.encClass || "";
      if (!procClass.includes(seg.proclitic.classe) || !encClass.includes(seg.enclitic.classe)) {
        continue;
      }
      const twvow = tw.voweledform;
      const endsI = twvow.endsWith("\u064A") || twvow.endsWith("\u064A\u0652") || twvow.endsWith("\u0650");
      if (enc.unvoweledform === "\u0647") enc.voweledform = endsI ? "\u0647\u0650" : "\u0647\u064F";
      else if (enc.unvoweledform === "\u0647\u0645\u0627") enc.voweledform = endsI ? "\u0647\u0650\u0645\u064E\u0627" : "\u0647\u064F\u0645\u064E\u0627";
      else if (enc.unvoweledform === "\u0647\u0645") enc.voweledform = endsI ? "\u0647\u0650\u0645\u0652" : "\u0647\u064F\u0645\u0652";
      else if (enc.unvoweledform === "\u0647\u0646") enc.voweledform = endsI ? "\u0647\u0650\u0646\u064E\u0651" : "\u0647\u064F\u0646\u064E\u0651";
      const lemma = tw.lemma;
      const rootRaw = tw.root || "";
      const root = rootRaw === removeAllDiacritics(rootRaw) ? rootRaw : "-";
      let voweled;
      if (["N1", "N2", "N3", "N5"].some((c) => procClass.includes(c))) {
        const res = twvow;
        if (isSolar(res[0])) {
          voweled = proc.voweledform + "\u0652" + res[0] + "\u0651" + res.slice(1) + enc.voweledform;
        } else {
          voweled = proc.voweledform + "\u0652" + res + enc.voweledform;
        }
      } else {
        voweled = proc.voweledform + twvow + enc.voweledform;
      }
      const pf = proc.voweledform === "" ? "#" : proc.voweledform + " : " + (proc.desc || "");
      const sf = enc.voweledform === "" ? "#" : enc.voweledform + " : " + (enc.desc || "");
      const typeStr = posList[Number(tw.pos) - 1];
      voweled = voweled.replaceAll("\u0644\u064E\u0644\u0652\u0644\u0651", "\u0644\u064E\u0644\u0651").replaceAll("\u0644\u0650\u0644\u0652\u0644\u0651", "\u0644\u0650\u0644\u0651");
      voweled = voweled.replaceAll("\u0644\u0650\u0627\u0644\u0651", "\u0644\u0650\u0644\u0651").replaceAll("\u0644\u064E\u0627\u0644\u0651", "\u0644\u064E\u0644\u0651");
      if (!notCompatible(normalizedWord, voweled)) {
        out.push(
          makeAnalysis(
            voweled,
            pf,
            removeLastDiacritics(twvow),
            typeStr,
            "-",
            "-",
            lemma,
            "-",
            root,
            "-",
            sf,
            "0.0001100101",
            {
              features: parsePosLabel(typeStr),
              proclitics: cliticAffixes(proc),
              enclitics: cliticAffixes(enc)
            }
          )
        );
      }
    }
    return out;
  }
  // ------------------------------------------------------------ propernoun
  analyzeProperNoun(normalizedWord, unvoweledWord, seg) {
    const procClass = seg.proclitic.classe;
    const encClass = seg.enclitic.classe;
    if (procClass.includes("V") || encClass.includes("V")) return [];
    const lex = this.lex;
    const unv = lex.map("propernoun_unvoweled").get(seg.stem);
    if (unv == null) return [];
    const voweledList = lex.list("propernoun_voweled");
    const posList = lex.list("propernoun_pos");
    const casemoodList = lex.list("propernoun_casemood");
    const proc = seg.proclitic;
    const enc = seg.enclitic;
    const out = [];
    for (const sid of unv.split(" ")) {
      const ipn = Number(sid);
      if (ipn > voweledList.length) continue;
      const pn = voweledList[ipn - 1];
      const pnvow = pn.vowform;
      const endsI = pnvow.endsWith("\u064A") || pnvow.endsWith("\u064A\u0652") || pnvow.endsWith("\u0650");
      if (enc.unvoweledform === "\u0647") enc.voweledform = endsI ? "\u0647\u0650" : "\u0647\u064F";
      else if (enc.unvoweledform === "\u0647\u0645\u0627") enc.voweledform = endsI ? "\u0647\u0650\u0645\u064E\u0627" : "\u0647\u064F\u0645\u064E\u0627";
      else if (enc.unvoweledform === "\u0647\u0645") enc.voweledform = endsI ? "\u0647\u0650\u0645\u0652" : "\u0647\u064F\u0645\u0652";
      else if (enc.unvoweledform === "\u0647\u0646") enc.voweledform = endsI ? "\u0647\u0650\u0646\u064E\u0651" : "\u0647\u064F\u0646\u064E\u0651";
      const pf = proc.voweledform;
      const sf = enc.voweledform;
      let voweled;
      if (proc.classe === "N4" && proc.unvoweledform.endsWith("\u0644") && pnvow.startsWith("\u0627")) {
        let val = pnvow;
        if (enc.unvoweledform !== "") val = val.replaceAll("\u0629", "\u062A");
        voweled = pf + val.slice(1) + sf;
      } else if (isDefinit(proc.classe)) {
        let res = pnvow;
        if (enc.unvoweledform !== "") res = res.replaceAll("\u0629", "\u062A");
        if (isSolar(res[0])) {
          voweled = pf + "\u0652" + res[0] + "\u0651" + res.slice(1) + sf;
        } else {
          voweled = pf + "\u0652" + res + sf;
        }
      } else {
        let res = pnvow;
        if (enc.unvoweledform !== "") res = res.replaceAll("\u0629", "\u062A");
        voweled = pf + res + sf;
      }
      if (notCompatible(normalizedWord, voweled, seg, false)) continue;
      const lemma = pn.lemma;
      const rootRaw = pn.root || "";
      const root = rootRaw === removeAllDiacritics(rootRaw) ? rootRaw : "-";
      const pfOut = pf === "" ? "#" : pf + " : " + (proc.desc || "");
      const sfOut = sf === "" ? "#" : sf + " : " + (enc.desc || "");
      const pos = posList[Number(pn.pos) - 1];
      const typeStr = [pos.main || "", pos.type || "", pos.gender || "", pos.number || ""].join(
        "|"
      );
      const cas = casemoodList[Number(pn.cas) - 1].valAR;
      if (_Analyzer.validProperNoun(seg, cas, voweled)) {
        out.push(
          makeAnalysis(
            voweled,
            pfOut,
            removeLastDiacritics(pnvow),
            typeStr,
            "-",
            "-",
            lemma,
            "-",
            root,
            cas,
            sfOut,
            "0.01100101",
            {
              features: {
                pos: pos.main,
                type: opt(pos.type),
                gender: opt(pos.gender),
                number: opt(pos.number)
              },
              state: opt(pos.definit),
              proclitics: cliticAffixes(proc),
              enclitics: cliticAffixes(enc)
            }
          )
        );
      }
    }
    return out;
  }
  static validProperNoun(seg, caseormood, voweled) {
    const pc = seg.proclitic.classe;
    if ((voweled.includes("\u064B") || voweled.includes("\u064D") || voweled.includes("\u064C")) && (seg.enclitic.unvoweledform !== "" || isDefinit(pc))) {
      return false;
    }
    if ((pc === "N2" || pc === "C2" || pc === "C3") && caseormood === "\u0645\u062C\u0631\u0648\u0631") return false;
    if ((pc === "N4" || pc === "N5") && caseormood !== "\u0645\u062C\u0631\u0648\u0631") return false;
    return true;
  }
  // --------------------------------------------------------- derived join
  getPossibleRoots(stem, rules, cat) {
    const roots = /* @__PURE__ */ new Set();
    for (const rule of rules.split(" ")) {
      let root = "";
      for (const r of rule) {
        if (NUMBERED.includes(r)) {
          const c = stem[Number(r) - 1];
          root += isHamza(c) ? "\u0621" : c;
        } else {
          root += isHamza(r) ? "\u0621" : r;
        }
      }
      if (this.lex.containsRoot(cat, root)) {
        roots.add(root);
      }
    }
    return roots;
  }
  getInfoResult(idRoot, idPatternStem, length, cat) {
    const result = [];
    const patternIds = new Set(idPatternStem.split(" "));
    const formulas = this.lex.formulas(cat, length);
    for (const idr of idRoot.split(" ")) {
      const idx = Number(idr);
      if (!(idx > 0 && idx <= formulas.length)) continue;
      const f = formulas[idx - 1];
      if (patternIds.has(f.idDiacPatternStem)) {
        result.push(f);
      }
    }
    return result;
  }
  possibleSolutions(seg, cat, cache) {
    const stem1 = seg.stem;
    const cached = cache.get(stem1);
    if (cached) return cached;
    const lex = this.lex;
    const stem = stem1 ? stem1[0] + normalizeHamza(stem1.slice(1)) : stem1;
    const lenDiac = stem.length;
    const result = /* @__PURE__ */ new Map();
    for (const unv of lex.unvoweledStems(cat, lenDiac)) {
      let sUnvoweled = unv.val;
      sUnvoweled = sUnvoweled[0] + normalizeHamza(sUnvoweled.slice(1));
      if (!isDiacPattern(stem, sUnvoweled)) continue;
      const iroots = this.getPossibleRoots(stem, unv.rules, cat);
      if (iroots.size === 0) continue;
      const rootsMap = /* @__PURE__ */ new Map();
      let add = false;
      for (const root of iroots) {
        const val = wordFromRootAndPattern(root, sUnvoweled);
        const rootEnt = lex.rootEntity(cat, root);
        if (rootEnt == null) continue;
        const lenStem = seg.stem.length;
        const idRoot = lenStem >= 1 && lenStem <= 12 ? rootEnt["len" + lenStem] : null;
        if (idRoot && normalizeHamza(val) === normalizeHamza(stem)) {
          const res = this.getInfoResult(idRoot, unv.ids, lenStem, cat);
          if (res.length) {
            rootsMap.set(root, res);
            add = true;
          }
        }
      }
      if (add) {
        result.set(unv.val, rootsMap);
      }
    }
    cache.set(stem1, result);
    return result;
  }
  static allRootFormulas(solutions) {
    const out = /* @__PURE__ */ new Map();
    for (const roots of solutions.values()) {
      for (const [root, formulas] of roots) {
        let bucket = out.get(root);
        if (!bucket) {
          bucket = /* @__PURE__ */ new Map();
          out.set(root, bucket);
        }
        for (const f of formulas) {
          bucket.set(f, f);
        }
      }
    }
    const final = /* @__PURE__ */ new Map();
    for (const [root, bucket] of out) {
      final.set(root, [...bucket.values()]);
    }
    return final;
  }
  infoResults(formula, length, cat) {
    const lex = this.lex;
    const out = new Array(10).fill("");
    let vd = lex.voweledDiacStem(cat, length, Number(formula.idDiacPatternStem));
    out[0] = vd.val;
    out[1] = vd.freq;
    {
      const [clen, cid] = formula.idCanonicPatternStem.split(".");
      vd = lex.voweledCanonicStem(cat, Number(clen), Number(cid));
      out[2] = vd.val;
      out[3] = vd.freq;
    }
    {
      const [llen, lid] = formula.idDiacPatternLemma.split(".");
      vd = lex.voweledDiacLemma(cat, Number(llen), Number(lid));
      out[4] = vd.val;
      out[5] = vd.freq;
    }
    {
      const [plen, pid] = formula.idCanonicPatternLemma.split(".");
      vd = lex.voweledCanonicLemma(cat, Number(plen), Number(pid));
      out[6] = vd.val;
      out[7] = vd.freq;
    }
    out[8] = formula.idPartOfSpeech;
    out[9] = formula.idCaseOrMood;
    return out;
  }
  // -------------------------------------------------------------- verbal
  analyzeVerbal(normalizedWord, seg) {
    const stem = seg.stem;
    const procClass = seg.proclitic.classe;
    const encClass = seg.enclitic.classe;
    if (!(stem.length >= 1 && stem.length <= 9) || procClass.includes("N") || encClass.includes("N")) {
      return [];
    }
    const lex = this.lex;
    const solutions = this.possibleSolutions(seg, "Verbs", this.verbCache);
    const imap = _Analyzer.allRootFormulas(solutions);
    const out = [];
    const lenStem = seg.stem.length;
    for (const [sRoot, formulas] of imap) {
      for (const sol of formulas) {
        const info = this.infoResults(sol, lenStem, "Verbs");
        const diac = info[0];
        const diacFreq = Number(info[1]);
        const canonicPattern = info[2];
        const canonicFreq = Number(info[3]);
        const lemma = wordFromRootAndPattern(sRoot, info[4]);
        const lemmaFreq = Number(info[5]);
        const lemmapattern = info[6];
        const lemmapatternFreq = Number(info[7]);
        const idpos = Number(info[8]);
        const feats = decodeVerb(lex, idpos);
        const posFreq = Number(feats.freq);
        const idcm = Number(info[9]);
        const cm = lex.casemood("Verbs", idcm).valAR;
        const voweled = vowelize(seg, diac);
        const voweledWord = voweled[0];
        if (!_Analyzer.validVerbal(
          seg,
          voweledWord,
          normalizedWord,
          feats.Type,
          feats.Voice,
          feats.Person2,
          cm,
          feats.Transitivity
        )) {
          continue;
        }
        const [procliticStr, encliticStr, prefix, suffix] = this.verbalInterpret(
          seg,
          feats.Type,
          feats.Person,
          feats.Person2,
          feats.Emphasized,
          cm
        );
        const stemH = correctStemHamza(seg.stem);
        const freq = (diacFreq + canonicFreq + lemmaFreq + lemmapatternFreq + posFreq) / 5;
        const priority = fmtFreq(freq);
        out.push(
          makeAnalysis(
            voweledWord,
            procliticStr,
            stemH,
            feats.partofspeech,
            diac,
            canonicPattern,
            lemma,
            lemmapattern,
            sRoot,
            cm,
            encliticStr,
            priority,
            {
              features: {
                pos: feats.Main,
                tense: feats.Type,
                voice: feats.Voice,
                person: feats.Person,
                pronoun: feats.Person2,
                transitivity: feats.Transitivity,
                augmentation: feats.Augmented
              },
              emphatic: feats.Emphasized === "\u0645\u0624\u0643\u062F",
              proclitics: cliticAffixes(seg.proclitic),
              prefixes: prefix ? [{ form: prefix[0], description: prefix[1] }] : [],
              suffixes: suffix ? [{ form: suffix[0], description: suffix[1] }] : [],
              enclitics: cliticAffixes(seg.enclitic)
            }
          )
        );
      }
    }
    return out;
  }
  static validVerbal(seg, voweledWord, normalizedWord, type_, _voice, person2, casemood, transitivity) {
    const pref = seg.proclitic.classe;
    const suff = seg.enclitic.classe;
    if (transitivity === "\u0644\u0627\u0632\u0645" && seg.enclitic.unvoweledform !== "") return false;
    if (pref === "V1" && type_ !== "\u0645\u0636\u0627\u0631\u0639" && casemood !== "\u0645\u0631\u0641\u0648\u0639") return false;
    if (pref === "V2" && type_ !== "\u0645\u0636\u0627\u0631\u0639" && casemood !== "\u0645\u0646\u0635\u0648\u0628") return false;
    if (pref === "V3" && type_ !== "\u0645\u0636\u0627\u0631\u0639" && casemood !== "\u0645\u062C\u0632\u0648\u0645") return false;
    if (pref === "C2" && type_ === "\u0623\u0645\u0631") return false;
    if ((suff === "V2" || suff === "V3") && type_ === "\u0623\u0645\u0631") return false;
    if (suff === "V4" && person2 !== "\u0623\u0646\u062A\u0645") return false;
    if (notCompatible(normalizedWord, voweledWord, seg, false)) return false;
    return true;
  }
  /**
   * Port of `VerbalAnalyzerImpl`'s proclitic/enclitic column assembly.
   *
   * Returns `[procliticStr, encliticStr, prefix, suffix]`: the two Java column
   * strings (`form|desc` items joined by `+`: clitic then the مضارع prefix;
   * subject suffix then clitic) plus the inflectional prefix / suffix as
   * `[form, desc]` pairs (`null` when absent).
   */
  verbalInterpret(seg, type_, person, person2, emphasized, casemood) {
    const prefix1 = seg.proclitic.voweledform === "" ? "#" : seg.proclitic.voweledform + "|" + (seg.proclitic.desc || "");
    const suffix1 = seg.enclitic.voweledform === "" ? "#" : seg.enclitic.voweledform + "|" + (seg.enclitic.desc || "");
    const prefix = _Analyzer.procliticValue(type_, person, person2);
    const suffix = _Analyzer.encliticValue(type_, person, person2, emphasized, casemood);
    let procliticStr;
    if (prefix !== null) {
      const p = prefix.join("|");
      procliticStr = prefix1 === "#" ? p : prefix1 + "+" + p;
    } else {
      procliticStr = prefix1;
    }
    let encliticStr;
    if (suffix !== null) {
      const s = suffix.join("|");
      encliticStr = suffix1 === "#" ? s : s + "+" + suffix1;
    } else {
      encliticStr = suffix1;
    }
    return [procliticStr, encliticStr, prefix, suffix];
  }
  /** The مضارع prefix letter and its label. */
  static procliticValue(type_, person, person2) {
    if (type_ === "\u0645\u0636\u0627\u0631\u0639") {
      if (person === "\u0645\u062E\u0627\u0637\u0628") return ["\u062A", "\u062D\u0631\u0641 \u0627\u0644\u0645\u0636\u0627\u0631\u0639\u0629"];
      if (person2 === "\u0623\u0646\u0627") return ["\u0623", "\u062D\u0631\u0641 \u0627\u0644\u0645\u0636\u0627\u0631\u0639\u0629"];
      if (person2 === "\u0646\u062D\u0646") return ["\u0646", "\u062D\u0631\u0641 \u0627\u0644\u0645\u0636\u0627\u0631\u0639\u0629"];
      if (person2 === "\u0647\u064A") return ["\u062A", "\u062A\u0627\u0621 \u0627\u0644\u063A\u0627\u0626\u0628\u0629"];
      if (person2 === "\u0647\u0645\u0627(\u0629)") return ["\u062A", "\u062D\u0631\u0641 \u0627\u0644\u0645\u0636\u0627\u0631\u0639\u0629"];
      return ["\u064A", "\u062D\u0631\u0641 \u0627\u0644\u0645\u0636\u0627\u0631\u0639\u0629"];
    }
    return null;
  }
  /** The subject / emphasis suffix and its label. */
  static encliticValue(type_, person, person2, emphasized, casemood) {
    if (emphasized === "\u0645\u0624\u0643\u062F") {
      return person === "\u0645\u062E\u0627\u0637\u0628" ? ["\u0646", "\u0646\u0648\u0646 \u0627\u0644\u062A\u0648\u0643\u064A\u062F"] : null;
    }
    if (type_ === "\u0623\u0645\u0631") {
      if (person === "\u0645\u062E\u0627\u0637\u0628" && person2 !== "\u0623\u0646\u062A\u064E") {
        if (person2 === "\u0623\u0646\u062A\u0650") return ["\u064A", "\u064A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u0629"];
        if (person2 === "\u0623\u0646\u062A\u0645\u0627") return ["\u0627", "\u0623\u0644\u0641 \u0627\u0644\u0645\u062B\u0646\u0649"];
        if (person2 === "\u0623\u0646\u062A\u0645") return ["\u0648\u0627", "\u0648\u0627\u0648 \u0627\u0644\u062C\u0645\u0627\u0639\u0629"];
        if (person2 === "\u0623\u0646\u062A\u0646") return ["\u0646", "\u0646\u0648\u0646 \u0627\u0644\u0646\u0633\u0648\u0629"];
        return null;
      }
      return null;
    }
    if (type_ === "\u0645\u0627\u0636") {
      const table = {
        \u0623\u0646\u0627: ["\u062A", "\u062A\u0627\u0621 \u0627\u0644\u0645\u062A\u0643\u0644\u0645"],
        \u0646\u062D\u0646: ["\u0646\u0627", "\u0646\u0648\u0646 \u0627\u0644\u0645\u062A\u0643\u0644\u0645\u064A\u0646"],
        \u0623\u0646\u062A\u064E: ["\u062A", "\u062A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628"],
        \u0623\u0646\u062A\u0650: ["\u062A", "\u062A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u0629"],
        \u0623\u0646\u062A\u0645\u0627: ["\u062A\u0645\u0627", "\u062A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u064A\u0646"],
        \u0623\u0646\u062A\u0645: ["\u062A\u0645", "\u062A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u064A\u0646"],
        \u0623\u0646\u062A\u0646: ["\u062A\u0646", "\u062A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u0627\u062A"],
        \u0647\u064A: ["\u062A", "\u062A\u0627\u0621 \u0627\u0644\u062A\u0623\u0646\u064A\u062B \u0627\u0644\u0633\u0627\u0643\u0646\u0629"],
        \u0647\u0645\u0627: ["\u0627", "\u0623\u0644\u0641 \u0627\u0644\u0627\u062B\u0646\u064A\u0646"],
        "\u0647\u0645\u0627(\u0629)": ["\u062A\u0627", "\u062A\u0627\u0621 \u0627\u0644\u062A\u0623\u0646\u064A\u062A \u0648\u0623\u0644\u0641 \u0627\u0644\u0627\u062B\u0646\u064A\u0646"],
        \u0647\u0645: ["\u0648\u0627", "\u0648\u0627\u0648 \u0627\u0644\u062C\u0645\u0627\u0639\u0629"],
        \u0647\u0646: ["\u0646", "\u0646\u0648\u0646 \u0627\u0644\u0646\u0633\u0648\u0629"]
      };
      return table[person2] ?? null;
    }
    if (type_ === "\u0645\u0636\u0627\u0631\u0639") {
      if (person2 === "\u0647\u0646" || person2 === "\u0623\u0646\u062A\u0646") return ["\u0646", "\u0646\u0648\u0646 \u0627\u0644\u0646\u0633\u0648\u0629"];
      if (person2 === "\u0623\u0646\u062A\u0650") {
        return casemood === "\u0645\u0631\u0641\u0648\u0639" ? ["\u064A\u0646", "\u064A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u0629 \u0648\u0627\u0644\u0646\u0648\u0646 \u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0631\u0641\u0639"] : ["\u064A", "\u064A\u0627\u0621 \u0627\u0644\u0645\u062E\u0627\u0637\u0628\u0629"];
      }
      if (person2 === "\u0623\u0646\u062A\u0645\u0627" || person2 === "\u0647\u0645\u0627(\u0629)" || person2 === "\u0647\u0645\u0627") {
        return casemood === "\u0645\u0631\u0641\u0648\u0639" ? ["\u0627\u0646", "\u0623\u0644\u0641 \u0627\u0644\u0645\u062B\u0646\u0649 \u0648\u0627\u0644\u0646\u0648\u0646 \u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0631\u0641\u0639"] : ["\u0627", "\u0623\u0644\u0641 \u0627\u0644\u0645\u062B\u0646\u0649"];
      }
      if (person2 === "\u0623\u0646\u062A\u0645" || person2 === "\u0647\u0645") {
        return casemood === "\u0645\u0631\u0641\u0648\u0639" ? ["\u0648\u0646", "\u0648\u0627\u0648 \u0627\u0644\u062C\u0645\u0627\u0639\u0629 \u0648\u0627\u0644\u0646\u0648\u0646 \u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0631\u0641\u0639"] : ["\u0648\u0627", "\u0648\u0627\u0648 \u0627\u0644\u062C\u0645\u0627\u0639\u0629"];
      }
      return null;
    }
    return null;
  }
  // -------------------------------------------------------------- nominal
  analyzeNominal(normalizedWord, seg) {
    const stem = seg.stem;
    const procClass = seg.proclitic.classe;
    const encClass = seg.enclitic.classe;
    if (!(stem.length >= 2 && stem.length <= 11) || procClass.includes("V") || encClass.includes("V")) {
      return [];
    }
    const lex = this.lex;
    const solutions = this.possibleSolutions(seg, "Nouns", this.nounCache);
    const imap = _Analyzer.allRootFormulas(solutions);
    const out = [];
    const lenStem = seg.stem.length;
    for (const [sRoot, formulas] of imap) {
      for (const sol of formulas) {
        const info = this.infoResults(sol, lenStem, "Nouns");
        const diac = info[0];
        const diacFreq = Number(info[1]);
        const canonicPattern = info[2];
        const canonicFreq = Number(info[3]);
        const lemma = wordFromRootAndPattern(sRoot, info[4]);
        const lemmaFreq = Number(info[5]);
        const lemmapattern = info[6];
        const lemmapatternFreq = Number(info[7]);
        const idpos = info[8] != null ? Number(info[8]) : 0;
        const feats = decodeNoun(lex, idpos);
        const posFreq = Number(feats.freq);
        const idcm = Number(info[9]);
        const cm = lex.casemood("Nouns", idcm).valAR;
        const voweled = vowelize(seg, diac);
        let voweledWord = voweled[0];
        if (feats.Number !== "\u0645\u062B\u0646\u0649" && voweledWord.endsWith("\u0621\u064E\u0627\u0646\u0650")) {
          voweledWord = voweledWord.replaceAll("\u0621\u064E\u0627\u0646\u0650", "\u0622\u0646\u0650");
        }
        voweledWord = voweledWord.replaceAll("\u0644\u064E\u0644\u0652\u0644\u0651", "\u0644\u064E\u0644\u0651").replaceAll("\u0644\u0650\u0644\u0652\u0644\u0651", "\u0644\u0650\u0644\u0651");
        const procDef = procClass === "N1" || procClass === "N2" || procClass === "N3" || procClass === "N5";
        let pDefinit = true;
        if (feats.Definit === "\u0645\u0639\u0631\u0641 \u0628\u0623\u0644" && !procDef) {
          pDefinit = false;
        }
        if (procDef && (feats.Definit === "\u0645\u0636\u0627\u0641 \u0625\u0644\u0649 \u0645\u0639\u0631\u0641\u0629" || feats.Definit === "\u0645\u0636\u0627\u0641 \u0625\u0644\u0649 \u0646\u0643\u0631\u0629" || feats.Definit === "\u063A\u064A\u0631 \u0645\u0636\u0627\u0641")) {
          pDefinit = false;
        }
        if (!pDefinit) continue;
        if (!_Analyzer.validNominal(seg, cm, voweledWord, normalizedWord)) continue;
        const [procliticStr, encliticStr, state, possible, suffixes] = _Analyzer.nominalInterpret(
          seg,
          feats.Number,
          feats.Gender,
          feats.Definit,
          canonicPattern
        );
        if (!possible) continue;
        const posStr = [feats.Main, feats.Type, feats.Number, feats.Gender, state].join("|");
        const stemH = correctStemHamza(seg.stem);
        const freq = (diacFreq + canonicFreq + lemmaFreq + lemmapatternFreq + posFreq) / 5;
        const priority = fmtFreq(freq);
        let t = "";
        if ((procClass === "N4" || procClass === "N5") && cm === "\u0645\u0646\u0635\u0648\u0628" && voweledWord === normalizedWord) {
          t = "\u0645\u062C\u0631\u0648\u0631";
        }
        out.push(
          makeAnalysis(
            voweledWord,
            procliticStr,
            stemH,
            posStr,
            diac,
            canonicPattern,
            lemma,
            lemmapattern,
            sRoot,
            t !== "" ? t : cm,
            encliticStr,
            priority,
            {
              features: {
                pos: feats.Main,
                type: feats.Type,
                gender: feats.Gender,
                number: feats.Number
              },
              state: opt(state),
              proclitics: cliticAffixes(seg.proclitic),
              suffixes: suffixes.map(([form, description]) => ({ form, description })),
              enclitics: cliticAffixes(seg.enclitic)
            }
          )
        );
      }
    }
    return out;
  }
  static validNominal(seg, caseormood, voweledWord, normalizedWord) {
    const pc = seg.proclitic.classe;
    const encUnv = seg.enclitic.unvoweledform;
    if ((voweledWord.includes("\u064B") || voweledWord.includes("\u064D") || voweledWord.includes("\u064C")) && (encUnv !== "" || isDefinit(pc))) {
      return false;
    }
    if ((pc === "N2" || pc === "C2" || pc === "C3") && caseormood === "\u0645\u062C\u0631\u0648\u0631") return false;
    if ((pc === "N4" || pc === "N5") && caseormood !== "\u0645\u062C\u0631\u0648\u0631" && voweledWord !== normalizedWord) {
      return false;
    }
    return !notCompatible(normalizedWord, voweledWord, seg, false);
  }
  /**
   * Port of `NominalAnalyzerImpl`'s proclitic/enclitic column assembly.
   *
   * Returns `[procliticStr, encliticStr, state, possible, suffixes]`: the two
   * Java column strings (`form: desc` items joined by ` + `: the feminine /
   * number suffixes then the enclitic), the definiteness state that ends the POS
   * string, whether the reading is admissible, and the suffixes as `[form, desc]`
   * pairs.
   */
  static nominalInterpret(seg, number, gender, definit, canonicPattern) {
    const unDiac = removeAllDiacritics(canonicPattern);
    const procClass = seg.proclitic.classe;
    let suffixes = [];
    let passe = true;
    let possible = true;
    const procliticStr = seg.proclitic.voweledform === "" ? "#" : seg.proclitic.voweledform + ": " + (seg.proclitic.desc || "");
    let encliticStr = seg.enclitic.voweledform === "" ? "#" : seg.enclitic.voweledform + ": " + (seg.enclitic.desc || "");
    const st = seg.stem;
    const ta = ["\u0629", "\u062A\u0627\u0621 \u0627\u0644\u062A\u0623\u0646\u064A\u062B"];
    if (gender === "\u0645\u0624\u0646\u062B" && number === "\u0645\u0641\u0631\u062F" && st.endsWith("\u0629")) {
      suffixes = [ta];
      passe = true;
    } else if (gender === "\u0645\u0624\u0646\u062B" && number === "\u0645\u062B\u0646\u0649") {
      if (st.endsWith("\u062A\u0627\u0646")) {
        suffixes = [ta, ["\u0627\u0646", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = true;
      } else if (st.endsWith("\u062A\u064A\u0646")) {
        suffixes = [ta, ["\u064A\u0646", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = true;
      } else if (st.endsWith("\u062A\u0627")) {
        suffixes = [ta, ["\u0627", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = false;
      } else if (st.endsWith("\u062A\u064A")) {
        suffixes = [ta, ["\u064A", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = false;
      }
    } else if (gender === "\u0645\u0624\u0646\u062B" && number === "\u062C\u0645\u0639") {
      if (st.endsWith("\u0627\u062A")) {
        suffixes = [["\u0627\u062A", "\u062A\u0627\u0621 \u0627\u0644\u062A\u0623\u0646\u064A\u062B"]];
        passe = true;
      }
    } else if (gender === "\u0645\u0630\u0643\u0631" && number === "\u0645\u062B\u0646\u0649") {
      if (st.endsWith("\u0627\u0646")) {
        suffixes = [["\u0627\u0646", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = true;
      } else if (st.endsWith("\u064A\u0646")) {
        suffixes = [["\u064A\u0646", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = true;
      } else if (st.endsWith("\u0627")) {
        suffixes = [["\u0627", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = false;
      } else if (st.endsWith("\u064A")) {
        suffixes = [["\u064A", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = false;
      }
    } else if (unDiac !== "\u0645\u0641\u0627\u0639\u0644" && canonicPattern !== "\u0641\u064E\u0639\u064E\u0627\u0644\u0650\u064A" && gender === "\u0645\u0630\u0643\u0631" && number === "\u062C\u0645\u0639") {
      if (st.endsWith("\u0648\u0646")) {
        suffixes = [["\u0648\u0646", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = true;
      } else if (st.endsWith("\u064A\u0646")) {
        suffixes = [["\u064A\u0646", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = true;
      } else if (st.endsWith("\u0648")) {
        suffixes = [["\u0648", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = false;
      } else if (st.endsWith("\u064A")) {
        suffixes = [["\u064A", "\u0639\u0644\u0627\u0645\u0629 \u0627\u0644\u0625\u0639\u0631\u0627\u0628"]];
        passe = false;
      }
    }
    let state;
    if (procClass === "N1" || procClass === "N2" || procClass === "N3" || procClass === "N5") {
      if (passe) {
        state = "\u0645\u0639\u0631\u0641";
      } else {
        state = "";
        possible = false;
      }
    } else {
      state = definit;
    }
    const suffix = suffixes.map(([form, desc]) => form + ": " + desc).join(" + ");
    if (encliticStr === "#" && suffix !== "") {
      encliticStr = suffix;
    } else if (encliticStr !== "#" && suffix !== "") {
      encliticStr = suffix + " + " + encliticStr;
    }
    return [procliticStr, encliticStr, state, possible, suffixes];
  }
};

// src/translit.ts
var BW_TO_AR = {
  "'": "\u0621",
  "|": "\u0622",
  ">": "\u0623",
  "&": "\u0624",
  "<": "\u0625",
  "}": "\u0626",
  A: "\u0627",
  b: "\u0628",
  p: "\u0629",
  t: "\u062A",
  v: "\u062B",
  j: "\u062C",
  H: "\u062D",
  x: "\u062E",
  d: "\u062F",
  "*": "\u0630",
  r: "\u0631",
  z: "\u0632",
  s: "\u0633",
  $: "\u0634",
  S: "\u0635",
  D: "\u0636",
  T: "\u0637",
  Z: "\u0638",
  E: "\u0639",
  g: "\u063A",
  _: "\u0640",
  f: "\u0641",
  q: "\u0642",
  k: "\u0643",
  l: "\u0644",
  m: "\u0645",
  n: "\u0646",
  h: "\u0647",
  w: "\u0648",
  Y: "\u0649",
  y: "\u064A",
  F: "\u064B",
  N: "\u064C",
  K: "\u064D",
  a: "\u064E",
  u: "\u064F",
  i: "\u0650",
  "~": "\u0651",
  o: "\u0652",
  "`": "\u0670",
  "{": "\u0671",
  P: "\u067E",
  J: "\u0686",
  V: "\u06A4",
  G: "\u06AF",
  R: "\u0698",
  ",": "\u060C",
  ";": "\u061B",
  "?": "\u061F"
};
var AR_TO_BW = {};
for (const [bw, ar] of Object.entries(BW_TO_AR)) {
  AR_TO_BW[ar] = bw;
}
function bwToArabic(word) {
  let out = "";
  for (const c of word) out += BW_TO_AR[c] ?? c;
  return out;
}

// src/lm.ts
var LM_FILE = "DATA.MSA.ALL.TRAIN.141809.lm";
var MAP_LEMMA = "DATA.MSA-LEMMA.ALL-train.map";
var MAP_STEM = "DATA.MSA.SHA.STEM.map";
var MAP_ROOT = "DATA.MSA.SHA.ROOT.map";
var lmCache;
function loadLm() {
  if (lmCache) return lmCache;
  const start = /* @__PURE__ */ new Map();
  const emission = /* @__PURE__ */ new Map();
  const transition = /* @__PURE__ */ new Map();
  const bw = bwToArabic;
  let sTotal = 0;
  const text = readGzipText(LM_FILE + ".gz");
  for (const raw of text.split("\n")) {
    const line = raw.replace(/[\r\n]+$/, "");
    if (!line) continue;
    const kind = line[0];
    if (kind === "S") {
      if (line[1] === "1") {
        const size = Number(line.slice(3, 5));
        const tag = line.slice(6, 6 + size);
        const tagFreq = Number(line.slice(7 + size));
        start.set(bw(tag), [sTotal, tagFreq]);
      } else {
        sTotal = Number(line.slice(3));
      }
    } else if (kind === "B") {
      const sizeT = Number(line.slice(2, 4));
      const sizeF = Number(line.slice(4, 5));
      const word = line.slice(6, 6 + sizeT);
      const wordFreq = Number(line.slice(7 + sizeT, 7 + sizeT + sizeF));
      const wordAr = bw(word);
      let pos = 8 + sizeT + sizeF;
      const n = line.length;
      while (pos < n) {
        const s2t = Number(line.slice(pos, pos + 2));
        const s2f = Number(line.slice(pos + 2, pos + 3));
        const tag = line.slice(pos + 4, pos + 4 + s2t);
        const tagFreq = Number(line.slice(pos + 4 + s2t, pos + 5 + s2t + s2f));
        emission.set(wordAr + ":" + bw(tag), tagFreq / wordFreq);
        pos += 6 + s2t + s2f;
      }
    } else if (kind === "A") {
      const sizeT = Number(line.slice(2, 4));
      const sizeF = Number(line.slice(4, 5));
      const tag1 = line.slice(6, 6 + sizeT);
      const tag1Freq = Number(line.slice(7 + sizeT, 7 + sizeT + sizeF));
      const tag1Ar = bw(tag1);
      let pos = 8 + sizeT + sizeF;
      const n = line.length;
      while (pos < n) {
        const s2t = Number(line.slice(pos, pos + 2));
        const s2f = Number(line.slice(pos + 2, pos + 3));
        const tag2 = line.slice(pos + 4, pos + 4 + s2t);
        const tag2Freq = Number(line.slice(pos + 4 + s2t, pos + 5 + s2t + s2f));
        transition.set(tag1Ar + ":" + bw(tag2), tag2Freq / tag1Freq);
        pos += 6 + s2t + s2f;
      }
    }
  }
  lmCache = { start, emission, transition };
  return lmCache;
}
var LanguageModel = class {
  get start() {
    return loadLm().start;
  }
  get emission() {
    return loadLm().emission;
  }
  get transition() {
    return loadLm().transition;
  }
  get mapLemma() {
    return loadMap(MAP_LEMMA);
  }
  get mapStem() {
    return loadMap(MAP_STEM);
  }
  get mapRoot() {
    return loadMap(MAP_ROOT);
  }
};

// src/disambiguate.ts
var BACKOFF = 1e-5;
var TOKEN_RESB = 1e-6;
var START_D = 0.5;
var START_MAXN_DEFAULT = 13336;
var WORD_RE = new RegExp("[" + ALL_ARABIC + "][" + ALL_ARABIC + ALL_DIACRITICS + "]*", "g");
var SENTENCE_SPLIT_RE = /[.!:،؟؛\n]/;
function tokenize(text) {
  text = text.replaceAll("\u0640", "");
  const sentences = [];
  for (const chunk of text.split(SENTENCE_SPLIT_RE)) {
    const tokens = (chunk.match(WORD_RE) ?? []).map((w) => correctErreur(w));
    if (tokens.length) sentences.push(tokens);
  }
  return sentences;
}
var Disambiguator = class {
  analyzer;
  lm;
  morphCache;
  constructor(analyzer, lm) {
    this.analyzer = analyzer ?? new Analyzer();
    this.lm = lm ?? new LanguageModel();
    this.morphCache = /* @__PURE__ */ new Map();
  }
  // ------------------------------------------------------------ morphology
  /**
   * `[lemmas, analyzed]` for `token` — dedup'd lemma list in analysis order,
   * mirroring `getAllLemmas`. Unanalyzable → `[[token], false]`.
   */
  lemmas(token) {
    const cached = this.morphCache.get(token);
    if (cached !== void 0) return cached;
    const analyses = this.analyzer._processToken(token);
    let res;
    if (analyses.length === 0) {
      res = [[token], false];
    } else {
      const seen = /* @__PURE__ */ new Set();
      const lemmas = [];
      for (const a of analyses) {
        if (!seen.has(a.lemma)) {
          seen.add(a.lemma);
          lemmas.push(a.lemma);
        }
      }
      res = [lemmas, true];
    }
    this.morphCache.set(token, res);
    return res;
  }
  backoff(lemma) {
    return this.lm.mapLemma.get(lemma) ?? BACKOFF;
  }
  // ------------------------------------------------------------- start row
  /** Port of the `startMatrix` block shared by analyzed/analyzedToken. */
  startMatrix(lemmas) {
    const start = this.lm.start;
    let maxStart = 1;
    let maxN = START_MAXN_DEFAULT;
    let som = 0;
    const raw = /* @__PURE__ */ new Map();
    for (let idx = 0; idx < lemmas.length; idx++) {
      const tag = lemmas[idx];
      let x = 0;
      const entry = start.get(tag);
      if (entry !== void 0) {
        const [total, freq] = entry;
        maxN = total;
        x = freq / total;
        const s = freq - START_D;
        if (idx === 0 || maxStart < s) {
          maxStart = s;
        }
      }
      som = idx === 0 ? x : som + x;
      raw.set(tag, x);
    }
    const size = lemmas.length || 1;
    const denom = som + maxStart / maxN * size;
    const out = /* @__PURE__ */ new Map();
    for (const [tag, x] of raw) {
      out.set(tag, (maxStart / maxN + x) / denom);
    }
    return out;
  }
  // --------------------------------------------------------------- decode
  /** One chosen lemma per token (parallel to `tokens`). */
  disambiguateLemmas(tokens) {
    if (tokens.length === 0) return [];
    const morph = /* @__PURE__ */ new Map();
    for (const t of tokens) morph.set(t, this.lemmas(t)[0]);
    if (tokens.length === 1) return this.analyzedToken(tokens[0], morph);
    return this.analyzed(tokens, morph);
  }
  analyzed(tokens, morph) {
    const n = tokens.length;
    const startMatrix = this.startMatrix(morph.get(tokens[0]));
    const emission = this.lm.emission;
    const transition = this.lm.transition;
    const backpointers = /* @__PURE__ */ new Map();
    let accum = /* @__PURE__ */ new Map();
    for (let i = 1; i < n; i++) {
      const word1 = tokens[i - 1];
      const word2 = tokens[i];
      const unv1 = removeAllDiacritics(word1);
      const unv2 = removeAllDiacritics(word2);
      const lem1 = morph.get(word1);
      const lem2 = morph.get(word2);
      const nextAccum = /* @__PURE__ */ new Map();
      const il = [];
      for (const tag2 of lem2) {
        let d1 = 0;
        let k = 0;
        for (let j = 0; j < lem1.length; j++) {
          const tag1 = lem1[j];
          const a = transition.get(tag1 + ":" + tag2);
          const resA = a !== void 0 ? a : this.backoff(tag1);
          let val;
          if (i === 1) {
            const b = emission.get(unv1 + ":" + tag1);
            const d2 = b !== void 0 ? b : this.backoff(tag1);
            val = resA + d2 + startMatrix.get(tag1);
          } else {
            val = resA + this.backoff(tag1);
          }
          if (j === 0 || val > d1) {
            d1 = val;
            k = j;
          }
        }
        const b2 = emission.get(unv2 + ":" + tag2);
        const resB = b2 !== void 0 ? b2 : this.backoff(tag2);
        nextAccum.set(tag2, resB + d1);
        il.push(k);
      }
      accum = nextAccum;
      backpointers.set(i, il);
    }
    const last = morph.get(tokens[n - 1]);
    let maxVal = 0;
    let imax = 0;
    for (let j = 0; j < last.length; j++) {
      const val = accum.get(last[j]);
      if (j === 0 || val > maxVal) {
        maxVal = val;
        imax = j;
      }
    }
    const chosen = [last[imax]];
    for (let i = n - 1; i > 0; i--) {
      imax = backpointers.get(i)[imax];
      chosen.push(morph.get(tokens[i - 1])[imax]);
    }
    chosen.reverse();
    return chosen;
  }
  /**
   * Single-token path. Note: Java iterates a `HashMap` keyset here, so its
   * tie-break is order-dependent; we iterate analysis order instead, which is
   * deterministic but may differ from Java on exact ties.
   */
  analyzedToken(token, morph) {
    const lemmas = morph.get(token);
    const sm = this.startMatrix(lemmas);
    const b = this.lm.emission.get(token);
    const resB = b !== void 0 ? b : TOKEN_RESB;
    let maxVal = 0;
    let maxTag = "";
    for (let idx = 0; idx < lemmas.length; idx++) {
      const tag = lemmas[idx];
      const val = sm.get(tag) + resB;
      if (idx === 0 || maxVal < val) {
        maxVal = val;
        maxTag = tag;
      }
    }
    return [maxTag];
  }
  // --------------------------------------------------------- stem / root
  /**
   * Highest-corpus-frequency `attr` (`stem`/`root`) among analyses sharing
   * `lemma` — port of `getStems` / `getRoots` (list order, first-on-tie).
   * Unanalyzable token → the token itself.
   */
  selectByFreq(token, lemma, analyzed, attr, freqMap) {
    if (!analyzed) return token;
    let best = null;
    let bestVal = 0;
    for (const a of this.analyzer._processToken(token)) {
      if (a.lemma === lemma) {
        const value = a[attr];
        const val = freqMap.get(value) ?? 0;
        if (best === null || bestVal < val) {
          best = value;
          bestVal = val;
        }
      }
    }
    return best !== null ? best : token;
  }
  // --------------------------------------------------------------- public
  /** Full per-token result: chosen lemma + best stem + best root. */
  disambiguate(tokens) {
    if (tokens.length === 0) return [];
    const lemmas = this.disambiguateLemmas(tokens);
    const out = [];
    for (let idx = 0; idx < tokens.length; idx++) {
      const token = tokens[idx];
      const lemma = lemmas[idx];
      const analyzed = this.lemmas(token)[1];
      const stem = this.selectByFreq(token, lemma, analyzed, "stem", this.lm.mapStem);
      const root = this.selectByFreq(token, lemma, analyzed, "root", this.lm.mapRoot);
      out.push({
        token,
        lemma: analyzed ? lemma : token,
        stem,
        // AlKhalil's "-" (no root) and its unanalyzable fallback (the token
        // itself) both mean "no root known".
        root: analyzed && root !== "-" ? root : null,
        analyzed
      });
    }
    return out;
  }
  /** Tokenize `text` and disambiguate each sentence; flat token order. */
  analyzeText(text) {
    const out = [];
    for (const sentence of tokenize(text)) {
      out.push(...this.disambiguate(sentence));
    }
    return out;
  }
};

// src/index.ts
var version = "0.3.0";
var defaultAnalyzer;
var defaultDisambiguator;
function getDefaultAnalyzer() {
  if (!defaultAnalyzer) defaultAnalyzer = new Analyzer();
  return defaultAnalyzer;
}
function getDefaultDisambiguator() {
  if (!defaultDisambiguator) defaultDisambiguator = new Disambiguator(getDefaultAnalyzer());
  return defaultDisambiguator;
}
function analyze(word) {
  return getDefaultAnalyzer().analyze(word);
}
function analyzeText(text) {
  return getDefaultDisambiguator().analyzeText(text);
}
window.Farahidi = {
  Analyzer,
  Disambiguator,
  analyze,
  analyzeText,
  hasRawText,
  layer1DataFiles,
  layer2DataFiles,
  provideRawText,
  tokenize,
  version
};
//# sourceMappingURL=index.js.map
})(typeof window !== "undefined" ? window : globalThis);
