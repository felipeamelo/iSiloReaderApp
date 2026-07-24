# Arquitectura do Leitor iSilo (app-isilo-reader)

## Visão Geral

Reimplementação de raiz em Java de um leitor de ficheiros iSilo (formato PDB com type="SDoc", creator="SilX") para Android API 21+. Baseado na engenharia reversa do APK original (BKP em `classes_dex_java/`) e do projecto Kotlin de referência (`app-isilo-modern/`).

```
┌─────────────────────────────────────────────────────────────┐
│                    CAMADA DE DADOS                          │
├─────────────────────────────────────────────────────────────┤
│ PalmDBImpl (PDB)  →  Lê ficheiro .pdb                      │
│   ├─ Open() → parseia header de 78 bytes                   │
│   │  - recordCount em offset 76-77 [✅]                    │
│   ├─ GetRecord(index) → dados crus do registo              │
│   └─ GetInfo() → type, creator, name                       │
│                                                             │
│ DocFormats                                                  │
│   └─ identifyFormat() → SDoc/SilX = iSilo 3/4 [✅]        │
│   └─ openFormat() → cria iSiloDoc [✅]                     │
├─────────────────────────────────────────────────────────────┤
│                   CAMADA DE HEADER                          │
├─────────────────────────────────────────────────────────────┤
│ SilXHeader (Record 0)                                       │
│   ├─ Header 128 bytes com magic "isSilo" (bytes 6-11) [✅] │
│   ├─ Campos: textSize, pageCount, fontSizes,               │
│   │  encodingFlags, docFlags, fontAttrs [✅]               │
│   ├─ Tabela A[0..3] (offsets de grupos) [✅]               │
│   └─ Tabelas k[]/B[] (record indices) [✅]                 │
├─────────────────────────────────────────────────────────────┤
│                  CAMADA DE DESCOMPRESSÃO                    │
├─────────────────────────────────────────────────────────────┤
│ HuffmanInflator (jq.java)                                   │
│   ├─ Bit reader 32-bit word big-endian, LSB-first [✅]     │
│   ├─ buildTree() com left/right/aux arrays [✅]            │
│   ├─ GetTrees() → lê nLit, nDist, nCL, constrói árvores   │
│   └─ InflateBlock() → LZ77 + Huffman + back-references [✅]│
│                                                             │
│ iSiloDoc (decompression)                                    │
│   ├─ open() → parseia Record 0 via SilXHeader [✅]         │
│   ├─ Lazy loading: descomprime 1 registo de cada vez [✅]  │
│   │  - Offset fixo 36 (após type/sub + block index)        │
│   │  - Fallback: busca word offsets 0..10 se 36 falhar    │
│   ├─ getText() → descomprime mais registos se necessário   │
│   └─ findString() → descomprime tudo para pesquisar [✅]   │
├─────────────────────────────────────────────────────────────┤
│                  CAMADA DE ESTILOS (FASE 3)                 │
├─────────────────────────────────────────────────────────────┤
│ Style Table do Grupo A[0]  [PENDENTE]                      │
│   ├─ Ler registo apontado por A[0]                         │
│   ├─ Parsear fx (4 bytes): count = d(2)                    │
│   ├─ Parsear fw (8 bytes each):                            │
│   │  d(0) = index no fontTable (0-12)                      │
│   │  f(2) / f(4) / f(6) = parâmetros de formato           │
│   └─ Mapear para TextStyle[]                                │
│                                                             │
│ StyleResolver                                               │
│   ├─ setFontTable(TextStyle[] 32) [parcial]                 │
│   │  - Hardcoded default [✅]                               │
│   │  - Da style table do documento [PENDENTE]              │
│   ├─ resolveText() com charset:                             │
│   │  - Latin-1: byte→char + Win-1252 0x80-0x9F [✅]       │
│   │  - UTF-8: decoder multi-byte [✅]                      │
│   │  - styleData/linkData dos blocos [PENDENTE]            │
│   └─ FormattedText + TextRun com TextStyle                 │
│                                                             │
│ IVDev.Font (original) → TextStyle (nosso)                   │
│   Original:                      Nosso:                     │
│   m_nID (1=Serif,2=Sans,...)    typeface (SERIF/SANS)      │
│   m_nSize (points)              fontSize (points)           │
│   m_nAttr bit 0 = bold          bold (boolean)              │
│   m_nAttr bit 1 = underline     underline (boolean)         │
│   m_nAttr bit 2 = strikethrough strikeThrough (boolean)     │
│   m_nAttr bit 3 = italic        italic (boolean)            │
│   m_nAttr bit 7 = hasTextColor  → m_abyTextRGB             │
│   m_nAttr bit 8 = hasBackColor  → m_abyBackRGB             │
├─────────────────────────────────────────────────────────────┤
│                   CAMADA DE LAYOUT                          │
├─────────────────────────────────────────────────────────────┤
│ LayoutEngine                                                │
│   ├─ layoutText() → word wrap [✅]                         │
│   │  - Mede char a char via CharMeasurer                   │
│   │  - Quebras em espaços e newlines                       │
│   └─ Gera LayoutLine[] (offset, length, y, width)          │
│                                                             │
│ DocumentLayout                                              │
│   ├─ layoutPages() → O(n) agora [✅]                       │
│   │  - Layout todas as linhas uma vez                      │
│   │  - Divide em páginas por altura                        │
│   └─ Page[] com startOffset, endOffset, lines              │
│                                                             │
│ AndroidCharMeasurer                                         │
│   ├─ measureCharWidth() com cache Typeface [✅]            │
│   ├─ measureTextWidth() em batch [✅]                      │
│   └─ getLineHeight/getBaseline [✅]                        │
├─────────────────────────────────────────────────────────────┤
│                 CAMADA DE RENDERIZAÇÃO (FASE 5)             │
├─────────────────────────────────────────────────────────────┤
│ ReadView                                                    │
│   ├─ onDraw() → try/catch [✅]                             │
│   ├─ drawContent():                                         │
│   │  - Fundo bege/creme [✅]                                │
│   │  - Header "Página X/Y" + título [✅]                   │
│   │  - Para cada linha:                                    │
│   │    1. findRunAtOffset() → TextRun                     │
│   │    2. applyStyle() → Paint                            │
│   │    3. canvas.drawText() [✅]                           │
│   └─ applyStyle() actual: [PENDENTE]                       │
│      - Typeface: 1=Serif, 2=Sans, 5=Mono                  │
│      - fontSize em pontos × density                        │
│      - bold/italic → Typeface.create()                     │
│      - underline/strikethrough → Paint                     │
│      - Cor de texto (m_abyTextRGB)                         │
│      - Cor de fundo (m_abyBackRGB)                         │
├─────────────────────────────────────────────────────────────┤
│                NAVEGAÇÃO E EXTRAS (FASE 6)                  │
├─────────────────────────────────────────────────────────────┤
│ Navegação                                                   │
│   ├─ nextPage/previousPage [✅]                            │
│   ├─ goToPage(page) [✅]                                   │
│   ├─ Touch: 1/3 esquerda=anterior, direita=próx [✅]      │
│   └─ Botões volume=navegação [✅]                          │
│                                                             │
│ Paginação [PENDENTE]                                        │
│   ├─ Extrair pageOffsets do Record 0                       │
│   ├─ Usar pageOffsets para boundaries                      │
│   └─ Lazy loading por página                               │
│                                                             │
│ TOC [PENDENTE]                                              │
│   ├─ Extrair TOC entries do Record 0                       │
│   └─ Menu "Índice" com lista                               │
│                                                             │
│ Imagens [PENDENTE]                                          │
│   ├─ Ler grupos A[1..3] (imagens)                          │
│   ├─ Descomprimir imagens (PNG/GIF/JPEG)                   │
│   └─ Renderizar no onDraw()                                │
│                                                             │
│ Links/Hyperlinks [PENDENTE]                                 │
│   ├─ Extrair link table dos blocos                         │
│   └─ Touch em links → navegar para offset                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Fluxo Completo

### 1. Abertura (DocListActivity → ReadActivity)

```
Utilizador toca num ficheiro .pdb
  ↓
DocListActivity envia filePath para ReadActivity
  ↓
ReadActivity.onCreate()
  └─ openFile(filePath)
      └─ loadDocument(filePath)
           ├─ FileDataStream.open(filePath, 0)  → abre ficheiro
           ├─ PalmDBImpl.Open(stream, 0)         → lê header PDB
           │   - name[32], type[4], creator[4]
           │   - recordCount = header[76..77]
           │   - recordOffsets[] da entry table
           ├─ DocFormats.openFormat(pdb)
           │   ├─ identifyFormat(pdb)
           │   │   ├─ GetInfo → type=SDoc, creator=SilX
           │   │   └─ retorna FORMAT_ISILO3/4
           │   └─ openISiloDoc(pdb, recordCount)
           │       ├─ GetRecord(0) → Record 0 bruto
           │       ├─ new iSiloDoc()
           │       └─ doc.openWithRecords(record0, records)
           │           ├─ open() → SilXHeader.parse()
           │           │   - Valida magic "isSilo" (bytes 6-11)
           │           │   - Lê campos header 128 bytes
           │           │   - Lê A[0..3], k[], B[]
           │           ├─ Guarda raw records para lazy loading
           │           └─ preloadFirstRecord() → descomprime 1º
           └─ doc.getInfo().title = PDB name
```

### 2. Layout (ReadView.openDocument)

```
readView.openDocument(doc)
  └─ textToFormatted(doc)
      ├─ getInfo() → textSize, charset
      ├─ getText(0, textSize, buffer)
      │   └─ lazy: descomprime registos se necessário
      ├─ StyleResolver.resolveText(buffer, textSize, styleData, charset)
      │   ├─ Se UTF-8: decodeUTF8()
      │   └─ Cria FormattedText com TextRun[]
      └─ new DocumentLayout(formattedText, measurer)
          └─ setPageMode(true)
              └─ requestLayout()
                  └─ onSizeChanged()
                      └─ layoutPages(width, height)
                          ├─ engine.layoutText() → LayoutLine[]
                          └─ Divide linhas em páginas
```

### 3. Renderização (onDraw)

```
onDraw(Canvas)
  └─ drawContent(canvas)
      ├─ canvas.drawPaint(bgPaint) → fundo bege
      ├─ drawText("Página X/Y") → header
      ├─ drawText(título) → canto superior direito
      ├─ Para cada linha na página:
      │   ├─ findRunAtOffset() → TextRun com TextStyle
      │   ├─ applyStyle(textPaint, run.style)
      │   │   ├─ Typeface (serif/sans/mono + bold/italic)
      │   │   ├─ textSize (points × density)
      │   │   ├─ setUnderlineText / setStrikeThruText
      │   │   └─ setColor (text / background)
      │   └─ canvas.drawText(run.text, x, y, textPaint)
      └─ (se houver imagens) drawBitmap() nas posições
```

### 4. Navegação

```
Touch na zona:
  ├─ 1/3 esquerda → previousPage()
  ├─ 1/3 direita → nextPage()
  └─ centro → (nada)

Volume up → previousPage()
Volume down → nextPage()
```

---

## Formato SilX (Record 0 — 128 bytes Header)

### Layout do Header (128 bytes)

| Offset | Tamanho | Campo | Leitura | Original |
|--------|---------|-------|---------|----------|
| 0 | 2 | headerSize | f(0) | Tamanho total do header |
| 2 | 1 | versionMajor | d(2) | Versão major (≥ 3) |
| 3 | 1 | versionMinor | d(3) | Versão minor |
| 4 | 1 | versionPatch | d(4) | Versão patch |
| 6 | 2 | "is" | a(0)=d(6)='i', a(1)=d(7)='s' | Magic prefix |
| 8 | 4 | "Silo" | b(0..3)=d(8..11) | Magic suffix |
| 12 | 2 | layoutInfo | f(12) | Layout flags |
| 14-19 | 6 | columnWidths | d(14..19) | Larguras de coluna |
| 20 | 4 | totalTextSize | g(20) | Tamanho total do texto |
| 24 | 2 | blockUnitSize | f(24) | Nº blocos por registo |
| 30 | 2 | pageCount | f(30) | Nº de páginas |
| 32 | 2 | titleFontSize | f(32) | Tamanho fonte título |
| 34 | 2 | bodyFontSize | f(34) | Tamanho fonte corpo |
| 36 | 2 | docFlags | f(36) | Flags do documento |
| 38 | 2 | encodingFlags | f(38) | Flags de encoding |
| 40 | 4 | crc | g(40) | Checksum |
| 48 | 4 | highNibble | g(48) & 0xF0000000 | Deve ser 0xF... |
| 52 | 1 | fontAttr1 | d(52) | Atributo fonte 1 |
| 53 | 1 | fontAttr2 | d(53) | Atributo fonte 2 |
| 56 | 2 | lastPageIndex | f(56) | Último índice de página |
| 58 | 2 | embeddingType | f(58) | Tipo de embedding (106=?) |

### Após o Header — Tabela de Grupos

```
offset headerSize (f(0)):
  [countA]:    16-bit — número de grupos A
  [A0]:        16-bit — offset do grupo 0 (record index)
  [A1]:        16-bit — offset do grupo 1
  [A2]:        16-bit — offset do grupo 2
  [A3]:        16-bit — offset do grupo 3
  (max 4 grupos, se countA > 4 os restantes são ignorados)

  [countK]:    16-bit — número de entradas k/B
  [k0..kN]:    16-bit cada — record starts (N = min(countK, 18))
  [B0..BN]:    16-bit cada — record counts (N = min(countK, 18))
```

### Record 0 — Metadados

O Record 0 contém:
- Header 128 bytes
- Tabela de grupos A/k/B
- Título do documento (string com tamanho prefixado)
- Tabela de estilos (referenciada por A[0])
- Tabela de páginas
- Tabela de imagens
- Tabela de TOC

---

## Formato dos Registos de Texto (Records 1+)

### Estrutura do Registo

```
Byte 0:      recType (0x04 = texto)
Byte 1:      recSub  (0x00 = texto comprimido)
                            0x01 = TOC/hyperlinks
                            0x02 = imagem?
                            0x03 = ?
                            0x04 = imagem?
                            0x05 = ?
                            0x06 = ?
                            0x07-08 = metadata pequena

Bytes 2-3:   ?? (possivelmente tamanho ou checksum)
Bytes 4-35:  Block index table + flags (para blockSize=8)

  Block Index Table:
    [0]: treeStartWord    (16-bit)
    [1]: treeEndWord      (16-bit) = blockStartWord[1]
    [2]: blockStartWord[1] (16-bit)
    [3]: blockStartWord[2] (16-bit)
    ...
    [blockSize+1]: blockStartWord[blockSize] (16-bit)

  Flags:
    [flagsOffset + bi]: flag byte para cada bloco
      bit 4 = 0 → comprimido
      bit 4 = 1 → stored (não comprimido)

Bytes 36+:   Huffman tree data + blocos comprimidos
```

### Descompressão de Cada Registo

1. Saltar bytes 0-35 (type/sub + block index + flags)
2. `GetTrees(data, treeStartWord*4, treeWords, 0)` → lê árvores Huffman
3. Para cada bloco `bi` de 1 a blockSize:
   - Se flag & 4: copiar bytes directamente: `(blockWords * 4) - (flag & 3)`
   - Senão: `InflateBlock(data, blockStartWord*4, blockWords, output)`

---

## Algoritmo de Descompressão (HuffmanInflator = jq.java)

### Bit Reader

- Lê palavras de 32 bits big-endian
- Bits lidos LSB-first dentro de cada palavra
- `readBit()`: testa `(word & mask)`, shift mask left, quando mask=0 lê nova palavra

### GetTrees

```
nLit = readBits(5) + 257  (máx 286)
nDist = readBits(5) + 1   (máx 30)
nCL = readBits(4) + 4     (máx 19)

CLLens[CL_ORDER[i]] = readBits(3) para i=0..nCL-1
buildTree(clLeft, clRight, CLLens, 19) → CL tree

LitLenLens = readCodeLens(CL tree, nLit) → 286 valores
buildTree(litLeft, litAux, litRight, LitLenLens, 286) → lit tree

DistLens = readCodeLens(CL tree, nDist) → 30 valores
buildTree(distLeft, distRight, DistLens, 30) → dist tree
```

### InflateBlock

```
loop:
  sym = decodeSym(litLeft, litRight, litAux, 286)
  if sym < 256: output byte sym
  if sym == 256: end of block (return)
  if sym > 256:  length/distance pair
    length = calculado de sym
    ds = decodeSym(distLeft, distRight, null, 30)
    distance = calculado de ds
    copy length bytes from outputPos - distance
```

### Tabelas de Length/Distance

```
LENGTH_BASE = {11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227}
LENGTH_EXTRA = (sym - 261) >> 2

DIST_BASE = {5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577}
DIST_EXTRA = (ds - 2) >> 1
```

---

## Mapeamento IVDev.Font → TextStyle

| IVDev.Font | TextStyle | Mapeamento |
|------------|-----------|------------|
| m_nID | typeface | 1=Typeface.SERIF, 2=Typeface.SANS_SERIF, 3/4=Typeface.DEFAULT, 5=Typeface.MONOSPACE |
| m_nSize | fontSize | pontos (multiplicar por density) |
| m_nAttr bit 0 | bold | Typeface.create(face, BOLD) |
| m_nAttr bit 1 | underline | Paint.setUnderlineText(true) |
| m_nAttr bit 2 | strikeThrough | Paint.setStrikeThruText(true) |
| m_nAttr bit 3 | italic | Typeface.create(face, ITALIC) |
| m_nAttr bit 7 | textColor | Paint.setColor(m_abyTextRGB) |
| m_nAttr bit 8 | backColor | Canvas.drawRect() com m_abyBackRGB |

---

## Tabela de Estilos (Grupo A[0])

### Estrutura fx (4 bytes)

```
pd(4 bytes):
  d(0) = ?
  d(1) = ?
  d(2) = número de entradas fw
  d(3) = ?
```

### Estrutura fw (8 bytes)

```
pd(8 bytes):
  d(0) = index no fontTable (0-12, mapeia para R[0..12])
  d(1) = ?
  f(2) = parâmetro 1 (16-bit)
  f(4) = parâmetro 2 (16-bit)
  f(6) = parâmetro 3 (16-bit)
```

---

## Pipeline de Encoding

```
Texto descomprimido (bytes crus)
  ↓
StyleResolver.resolveText()
  ├─ charset = CHARSET_UTF8 (2) → decodeUTF8()
  │   Multi-byte UTF-8 → char[] Unicode
  └─ charset = CHARSET_LATIN1 (1) → byte→char directo
      Bytes 0x80-0x9F mapeados via WIN1252_MAP[]
  ↓
FormattedText com TextRun[]
  └─ TextRun.text (char[]) + TextStyle
```

---

## Ficheiros do Projecto

```
app/src/main/java/com/dcco/app/iSilo/

engine/
├── PalmDB.java                    (abstract)
├── data/
│   ├── DataStream.java           (abstract)
│   └── FileDataStream.java       (RandomAccessFile)
├── format/
│   ├── DocFormat.java            (abstract)
│   ├── iSiloDocInfo.java         (metadados)
│   ├── iSiloDoc.java             (iSilo 3/4 + lazy loading)
│   ├── iSilo2Doc.java            (iSilo 1/2 - esqueleto)
│   ├── DocDoc.java               (PalmDoc - esqueleto)
│   ├── TxtDoc.java               (texto puro - esqueleto)
│   ├── DocHeader.java            (old - para iSilo 3)
│   ├── SilXHeader.java           (SilX Record 0 parser)
│   ├── DocFormats.java           (identificação + factory)
│   └── PalmDBImpl.java           (leitor PDB)
├── render/
│   ├── CharMeasurer.java         (interface)
│   ├── AndroidCharMeasurer.java  (Paint + cache Typeface)
│   ├── TextStyle.java            (fontSize, bold, italic...)
│   ├── TextRun.java              (char[] + offset + length + style)
│   ├── FormattedText.java        (lista de TextRun[])
│   ├── LayoutEngine.java         (word wrap)
│   ├── LayoutLine.java           (linha com offset/y/width)
│   ├── Page.java                 (página com linhas)
│   ├── DocumentLayout.java       (layout paginado O(n))
│   └── StyleResolver.java        (byte→char + TextRun creation)
├── util/
│   ├── DebugLog.java             (logging detalhado)
│   ├── ErrorUtil.java            (tratamento de erros)
│   ├── HuffmanInflator.java      (jq.java port)
│   ├── iSiloInflator.java        (abstract)
│   └── iSiloDecompress.java      (não usado - LZ77 antigo)
└── state/
    └── BinarySettings.java       (settings)

ui/
├── reader/
│   ├── ReadActivity.java         (Activity + menu Copiar Log)
│   ├── ReadView.java             (View + onDraw + navegação)
│   └── FindActivity.java         (diálogo de pesquisa)
├── doclist/
│   └── DocListActivity.java      (lista de .pdb)
└── iSiloActivityGroup.java       (deprecated)
```

---

## Estado Actual

| Componente | % | Status |
|---|---|---|
| PalmDBImpl | 100% | ✅ Completo |
| DocFormats | 100% | ✅ Completo |
| SilXHeader | 95% | ✅ Completo (falta título real do Record 0) |
| HuffmanInflator | 100% | ✅ Completo |
| iSiloDoc decompress | 95% | ✅ Lazy loading |
| Style Table (A[0]) | 0% | ❌ Pendente |
| StyleResolver | 60% | 🔶 Parcial (falta styleData) |
| LayoutEngine | 100% | ✅ Completo |
| DocumentLayout | 100% | ✅ Completo |
| AndroidCharMeasurer | 100% | ✅ Completo |
| ReadView rendering | 70% | 🔶 Parcial (faltam estilos visuais) |
| Navegação | 100% | ✅ Completo |
| Paginação | 0% | ❌ Pendente |
| Imagens | 0% | ❌ Pendente |
| TOC | 0% | ❌ Pendente |
| Links | 0% | ❌ Pendente |

### Lenda
- `✅` = Implementado e funcional
- `🔶` = Parcial (funciona mas incompleto)
- `❌` = Não iniciado
