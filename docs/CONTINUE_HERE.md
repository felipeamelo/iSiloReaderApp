# Projecto Leitor iSilo Android — Ponto de Retomada

## Gerado em: 2026-07-24
## Última sessão: completa (Fases 0-5 implementadas)

---

## 1. DIRECTORIAS E ACESSOS

### Projecto principal
```
/mnt/c/PROJETOS/ISILO/app-isilo-reader/
```

### Ficheiros de referência (BKP — original decompilado)
```
/mnt/c/PROJETOS/ISILO/BKP/classes_dex_java/sources/com/dcco/app/iSilo/
  └─ ny.java      -> Handler principal do formato SDoc/SilX (8468 linhas)
  └─ jq.java      -> Descompressão Huffman+LZ77 (245 linhas, mal decompilado)
  └─ no.java      -> Handler do formato DSet/Silo
  └─ nu.java      -> Handler do formato ToGo (iSilo 1/2)
  └─ IVDev.java   -> Renderizador de texto (Paint, Typeface, cores)
  └─ IVDoc.java   -> Documento lógico (getText, encoding, fontes)
  └─ ml.java      -> TextOut (desenha texto no Canvas)
  └─ aa.java      -> Utilitários de bytes (read16, read32)
  └─ pd.java      -> Base class para buffers (d(), f(), g(), e(), a(), b())
  └─ el.java      -> Header 128 bytes (J=128)
  └─ en.java      -> Header 128 bytes (J=128)
  └─ em.java      -> Header 64 bytes + a()/b() offset helpers
  └─ fx.java      -> 4-byte header (grupos)
  └─ fw.java      -> 8-byte entry (style table)
  └─ fd.java      -> 4-byte header (page tree)
  └─ eb.java      -> 16-byte buffer (page tree data + delta decoder)
  └─ nz.java      -> Nó de page tree
  └─ oh.java      -> Page tree holder (2 ob + record buffer)
  └─ ob.java      -> Page tree subtree (oi + oc)
  └─ oi.java      -> Subtree node (nz + oc)
  └─ oc.java      -> Subtree data
```

### Projecto Kotlin (referência, não copiar)
```
/mnt/c/PROJETOS/ISILO/app-isilo-modern/
  └─ JqDecompressor.kt -> Port do jq.java (contém bugs segundo o utilizador)
```

### Docs do projecto
```
/mnt/c/PROJETOS/ISILO/app-isilo-reader/docs/
  └─ architecture.md       -> Arquitectura detalhada
  └─ summary.md            -> Estado do desenvolvimento
  └─ CONTINUE_HERE.md      <- ESTE FICHEIRO
  └─ iSilo.chm             -> Manual do utilizador iSilo Desktop (não tem spec técnica)
```

### Ficheiros de teste
```
/mnt/c/PROJETOS/ISILO/app-isilo-reader/app/src/main/java/...  (código fonte)
/mnt/c/PROJETOS/ISILO/iSiloDocs/
  └─ Oracoes Catolicas.pdb -> Ficheiro pequeno (~30 registos, ~54 páginas)
  └─ Missal v7-1.pdb      -> Ficheiro grande (~2000+ páginas, com links)
```

### Android SDK
```
/home/felipemelo/Android/Sdk/
  └─ platforms/   (android-34, android-36)
  └─ build-tools/ (34.0.0, 35.0.0, 36.0.0)
  └─ platform-tools/adb
```

### Build tool (Gradle)
```
/tmp/gradle-8.5/bin/gradle
```
Comando: `ANDROID_HOME=/home/felipemelo/Android/Sdk /tmp/gradle-8.5/bin/gradle assembleDebug`

---

## 2. ESTRUTURA DO CÓDIGO

```
app/src/main/java/com/dcco/app/iSilo/

engine/                           ← Lógica central
├── PalmDB.java                   (abstract) Interface PDB
├── data/
│   ├── DataStream.java           (abstract) Stream de dados
│   └── FileDataStream.java       RandomAccessFile implementation
├── format/                       ← Parsing de formatos
│   ├── DocFormat.java            (abstract) Base para documentos
│   ├── iSiloDocInfo.java         Metadados: title, charset, textSize, pageOffsets, links, TOC
│   ├── iSiloDoc.java             iSilo 3/4: lazy loading, descompressão, links
│   ├── iSilo2Doc.java            iSilo 1/2: esqueleto
│   ├── DocDoc.java               PalmDoc: esqueleto
│   ├── TxtDoc.java               Texto puro: esqueleto
│   ├── DocHeader.java            Antigo parser (iSilo 3) — substituído por SilXHeader
│   ├── SilXHeader.java           Parser do Record 0 SilX (128 bytes + grupos A/k/B)
│   ├── DocFormats.java           Factory: identifyFormat() + openFormat() + extração
│   ├── PalmDBImpl.java           Leitor PDB (recordCount em bytes 76-77)
│   └── LinkEntry.java            DTO para links
├── render/                       ← Layout e renderização
│   ├── CharMeasurer.java         (interface) Medição de caracteres
│   ├── AndroidCharMeasurer.java  Paint + Typeface cache
│   ├── TextStyle.java            fontId, fontSize, fontColor, bgColor, typeface, bold, italic...
│   ├── TextRun.java              char[] + offset + length + style
│   ├── FormattedText.java        Lista de TextRun[]
│   ├── LayoutEngine.java         Word wrap O(n)
│   ├── LayoutLine.java           Linha: charOffset, charLength, y, width, height
│   ├── Page.java                 Página: startOffset, endOffset, array de LayoutLine
│   ├── DocumentLayout.java       Layout incremental (chunks 2500 chars) + pageOffsets
│   └── StyleResolver.java        byte→char (UTF-8/Latin-1) + TextRun creation
├── util/
│   ├── DebugLog.java             Logging detalhado + clipboard
│   ├── ErrorUtil.java            isError() helper
│   ├── HuffmanInflator.java      Descompressão jq.java (32-bit word, árvore binária)
│   ├── iSiloInflator.java        (abstract) Base para HuffmanInflator
│   └── iSiloDecompress.java      NÃO USADO (LZ77 antigo, incorreto)
└── state/
    └── BinarySettings.java       Settings

ui/                               ← Interface Android
├── reader/
│   ├── ReadActivity.java         Activity principal: menu (TOC, Procurar, Copiar Log)
│   ├── ReadView.java             View personalizada: onDraw, navegação, links
│   └── FindActivity.java         Diálogo de pesquisa
├── doclist/
│   └── DocListActivity.java      Lista de ficheiros .pdb
└── iSiloActivityGroup.java       (deprecated)
```

---

## 3. ESTADO ACTUAL

```
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓  COMPLETO (✅):                                ▓
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓

  FASE 0 — Crash ANR
  ├── Cache Typeface em AndroidCharMeasurer      ✅
  ├── try/catch em onSizeChanged                 ✅
  ├── try/catch em onDraw                        ✅
  ├── DocumentLayout O(n) em vez de O(n²)        ✅
  └── Validação contentW/contentH > 0            ✅

  FASE 1 — Record 0 SilX
  ├── SilXHeader com magic "isSilo" bytes 6-11   ✅
  ├── headerSize, textSize, pageCount, fontSizes ✅
  ├── encodingFlags, docFlags, fontAttrs         ✅
  ├── Tabela A[0..3], k[0..17], B[0..17]         ✅
  └── Título do PDB name                         ✅

  FASE 2 — Block Index Table (descompressão)
  ├── HuffmanInflator bit reader 32-bit word     ✅
  ├── buildTree() left/right/aux                 ✅
  ├── GetTrees() + InflateBlock()                ✅
  ├── Offset fixo 36 + fallback 0..10            ✅
  ├── Múltiplos blocos por registo               ✅
  └── Lazy loading (descomprime sob demanda)     ✅

  FASE 4 — Encoding
  ├── Detecção UTF-8 heurística                  ✅
  ├── Decoder UTF-8 multi-byte                   ✅
  └── Windows-1252 remapping 0x80-0x9F           ✅

  FASE 5 — Renderização
  ├── Typeface mapping (Serif, Sans, Mono)       ✅
  ├── bold/italic/underline/strikethrough        ✅
  ├── Cor de texto (fontColor)                   ✅
  ├── Fundo de texto (bgColor)                   ✅
  └── fontSize em pontos × density               ✅

  FASE 6a — Navegação
  ├── nextPage/previousPage                      ✅
  ├── Touch 1/3 esquerda/direita                 ✅
  ├── Volume up/down                             ✅
  └── goToPage()                                 ✅

  EXTRAS
  ├── DebugLog + Copiar Log                      ✅
  ├── Auto-cópia do log no onDestroy()           ✅
  └── iSiloDocInfo com todos os metadados        ✅

▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓  EM ANDAMENTO / PARCIAL (🔶):                   ▓
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓

  FASE 6b — Page Offsets
  ├── Leitura de registos k[2]/k[3] (page tree)  🔶
  ├── Parsing de estrutura fd (4 bytes)           🔶
  ├── Parsing de estrutura eb (16 bytes)          🔶
  ├── Delta decoder (cases 0,1,2,3)              🔶
  └── Integração com DocumentLayout               🔶
  OBS: Funciona para o Oracoes (fallback),
       precisa testar com Missal (page tree real)

  FASE 6c — TOC
  ├── Extracção de grupo k[1]/B[1]               🔶
  ├── Parser de entradas (title + offset)        🔶
  └── Menu "Índice" na ReadActivity              🔶
  OBS: k[1]=0 no Oracoes, menu não aparece.
       Precisa testar com Missal.

  FASE 6d — Links
  ├── Identificação de registos sub=0x01          🔶
  └── Parser de entradas                          🔶
  OBS: Formato desconhecido. Dump hex feito.
       Touch detection implementado mas não
       usado porque os links não são parseados.

▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓  PENDENTE (⬜):                                   ▓
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓

  ⬜ Links          — Formato sub=0x01 por descobrir
  ⬜ Formatação     — Estilos por bloco (flag bytes)
  ⬜ Imagens        — Grupos A[1..3]
  ⬜ TOC real       — Grupo k[1]/B[1] no Missal
  ⬜ Lazy loading   — Usar pageOffsets para saltar páginas
  ⬜ Cache LRU      — Manter N páginas na memória
  ⬜ Modo contínuo  — Scroll em vez de páginas
  ⬜ Ajuste fonte   — Configuração de tamanho/face
  ⬜ Temas          — Nocturno, sépia
  ⬜ Anotações      — Inline annotations
  ⬜ Marcadores     — Bookmarks
```

---

## 4. TENTATIVAS QUE FALHARAM

| O que tentámos | Porque falhou | Solução |
|---|---|---|
| `DocHeader.parse()` com formato iSilo 3 | SilX tem estrutura 128-byte diferente | Criámos `SilXHeader.java` |
| Block Index via `rd[0]` como headerStart | groupCount=0, sem block index | Brute-force com scoring |
| `java.util.zip.Inflater(true)` | iSilo não usa DEFLATE standard | Implementámos `jq.java` |
| `iSiloDecompress.java` (LZ77) | Formato LZ77 errado | Só `HuffmanInflator` é usado |
| Copiar `JqDecompressor.kt` | Projecto Kotlin está "incompleto e falho" | Usar só código original BKP |
| recordCount em bytes 72-73 ou 74-75 | Original usa offset 76-77 | `recordCount = header[76..77]` |
| TYPE="SILO"/"SilX" | Ficheiros reais têm TYPE="SDoc" | `identifyFormat()` usa SDoc |
| Links sub=0x01 como pairs | Formato diferente | Dump hex para análise |
| DocumentLayout O(n²) por página | ANR para texto grande | Layout incremental O(n) |

---

## 5. FORMATO SILX — REFERÊNCIA RÁPIDA

### Record 0 (128 bytes header)

| Offset | Campo | Leitura |
|--------|-------|---------|
| 0 | headerSize | f(0) |
| 2 | versionMajor | d(2) |
| 6-11 | magic "isSilo" | d(6..11) |
| 20 | totalTextSize | g(20) |
| 24 | blockUnitSize | f(24) |
| 30 | pageCount | f(30) |
| 32 | titleFontSize | f(32) |
| 34 | bodyFontSize | f(34) |
| 36 | docFlags | f(36) |
| 38 | encodingFlags | f(38) |
| 52 | fontAttr1 | d(52) |
| 53 | fontAttr2 | d(53) |
| 58 | embeddingType | f(58) |

### Após o header — Grupo A

```
offset f(0):  countA (16-bit)
offset+2:     A[0] (16-bit) — style table
offset+4:     A[1] (16-bit) — page tree 0
offset+6:     A[2] (16-bit) — page tree 1
offset+8:     A[3] (16-bit) — images

Após grupos A: countK (16-bit) + k[0..17] + B[0..17]
```

### Records de texto (type=0x04)

| sub | Conteúdo |
|-----|----------|
| 0x00 | Texto comprimido (Huffman+LZ77) |
| 0x01 | Links/TOC (formato por confirmar) |
| 0x02 | Imagens ou dados de estilo |
| 0x03-0x08 | Metadata, end markers |

### Page tree (records k[2]/k[3])

```
Record byte[0]:        headerStart
byte[headerStart]:     fd header (4 bytes)
  fd.d(0) = offset to data table
  fd.d(3) = subtrees - 1
headerStart + fd.d(0): subtree entries (4 bytes each)
  [16-bit offset] [16-bit count]

Subtree data:
  eb buffer (16 bytes header):
    d(0) = data offset within eb
    d(2) & 3 = encoding type
    f(6) = number of entries
    g(12) = last offset
  At d(0): delta-encoded offsets
```

### HuffmanInflator constants

```
LENGTH_BASE = {11,13,15,17,19,23,27,31,35,43,51,59,67,83,99,115,131,163,195,227}
DIST_BASE   = {5,7,9,13,17,25,33,49,65,97,129,193,257,385,513,769,1025,1537,2049,3073,4097,6145,8193,12289,16385,24577}
CL_ORDER    = {16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15}
```

---

## 6. PRÓXIMO PASSO RECOMENDADO

1. **Testar Missal v7-1.pdb** — o ficheiro grande com links e TOC reais
2. **Analisar dump hex dos links** (registos sub=0x01) — descobrir formato
3. **Verificar page offsets** do Missal (logs PAGE_TREE)
4. **Implementar navegação por links** quando formato for compreendido
5. **Extrair TOC do Missal** se k[1]/B[1] estiverem preenchidos
6. **Implementar formatação** por bloco (bold, italic a partir dos flag bytes)

---

## 7. COMANDOS ÚTEIS

```bash
# Build
ANDROID_HOME=/home/felipemelo/Android/Sdk /tmp/gradle-8.5/bin/gradle assembleDebug

# Install (com device ligado)
/home/felipemelo/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# Ver estrutura de ficheiros
find . -name "*.java" | sort

# Ver BKP original
ls -la /mnt/c/PROJETOS/ISILO/BKP/classes_dex_java/sources/com/dcco/app/iSilo/
```
