# Sumário do Desenvolvimento — Leitor iSilo Android

## 1. O QUE FUNCIONA (✅)

### Leitura de Ficheiros PDB
- **PalmDBImpl**: Leitura de headers PDB (78 bytes), recordCount em offset 76-77, extract de registos
- **DocFormats.identifyFormat()**: Reconhece TYPE="SDoc" + CREATOR="SilX" como iSilo 3/4
- **DocFormats.identifyFormat()**: Reconhece TYPE="ToGo" como iSilo 1/2 (estrutura)
- **DocFormats.identifyFormat()**: Reconhece TYPE="TEXt" como PalmDoc (estrutura)

### Parsing do Record 0 (SilXHeader)
- Magic "isSilo" validado nos bytes 6-11
- 128-byte header parseado: textSize, pageCount, fontSizes, encodingFlags, docFlags, fontAttrs
- Tabela de grupos A[0..3], k[0..17], B[0..17] extraída após o header de 128 bytes
- Título extraído do PDB name (bytes 0-31 do header PDB)

### Descompressão (HuffmanInflator)
- Bit reader: palavras de 32 bits big-endian, LSB-first
- Árvore binária com arrays left/right/aux (igual ao `jq.java` original)
- `GetTrees()`: lê nLit, nDist, nCL, constrói árvores Huffman
- `InflateBlock()`: LZ77 + Huffman com length/distance (LENGTH_BASE/DIST_BASE do original)
- Constantes: CL_ORDER, LENGTH_BASE, DIST_BASE — idênticas ao `jq.java` original

### Lazy Loading
- `iSiloDoc.openWithRecords()` guarda registos crus, não descomprime tudo
- `preloadFirstRecord()`: descomprime só o primeiro registo de texto para abrir rápido
- `getText()`: se o offset pedido ainda não foi descomprimido, descomprime mais registos
- `findString()`: força descompressão completa se necessário para pesquisa

### Layout e Renderização
- **LayoutEngine**: word wrap com quebras em espaços e newlines
- **DocumentLayout**: layout incremental em chunks de 2500 caracteres
  - `getPage(N)`: constrói página N + pré-carrega mais um chunk
  - `setPageOffsets()`: se existirem page offsets do documento, usa boundaries fixas
- **AndroidCharMeasurer**: cache de Typeface por estilo (não recria a cada char)
- **ReadView.onDraw()**: desenha fundo bege, header "Página X", texto
- **applyStyle()**: bold, italic, underline, strikethrough, fontSize, fontColor, typeface mapping

### Navegação
- nextPage/previousPage
- Touch: 1/3 esquerda ← anterior, 1/3 direita → próximo
- Volume up/down para navegação
- Menu "Procurar" (FindActivity)
- Menu "Copiar Log"
- Menu "Índice" (TOC) — se existir no documento

### Estabilidade e Debug
- try/catch em onDraw() — erros de renderização não crasham a app
- try/catch em onSizeChanged() — erros de layout não crasham
- try/catch em openFile() — erros de abertura são tratados
- DebugLog: sistema de logging detalhado, copiável para clipboard
- Auto-cópia do log no onDestroy()

### Extracção de Page Offsets
- Leitura de registos k[2] e k[3] (page tree records)
- Parsing de estrutura `fd` (4 bytes) para obter sub-árvores
- Parsing de estrutura `eb` (16 bytes) para obter número de entradas e tipo de encoding
- Decodificador de deltas nos 4 formatos (cases 0, 1, 2, 3 do original `ny.java.a(nz)`)
- Fallback: divisão igual do texto por altura do ecrã se não houver page tree

### Encoding
- Detecção de UTF-8 por heurística (sequências multi-byte válidas)
- Decoder UTF-8 completo (1-4 bytes por caractere, surrogate pairs para > U+FFFF)
- Windows-1252 remapping para bytes 0x80-0x9F

---

## 2. O QUE TENTÁMOS E NÃO FUNCIONOU (❌)

### Abordagem Inicial — DocHeader Original
- **Tentativa**: Usar `DocHeader.parse()` com offset para iSilo 3 (versão, flags, totalTextSize, recordCount, recOffsetBits)
- **Resultado**: O formato SilX (iSilo 4) tem uma estrutura de Record 0 completamente diferente (128-byte header com magic "isSilo" nos bytes 6-11)
- **Solução**: Criámos `SilXHeader.java` que parseia o formato SilX correctamente

### Block Index Table via rd[0] (headerStart)
- **Tentativa**: Usar `rd[0]` como headerStart e parsear block index table (treeStartWord, treeEndWord, blockStartWord[], flags[])
- **Resultado**: O grupo A[0..3] tem count=0 para o ficheiro testado, e a block index table não foi encontrada — produziu 0 output
- **Solução**: Implementámos brute-force com scoring (% de chars imprimíveis) para encontrar offsets correctos

### Inflater (java.util.zip)
- **Tentativa**: Usar `java.util.zip.Inflater(true)` para raw DEFLATE, assumindo que iSilo usa DEFLATE standard
- **Resultado**: DataFormatException — o iSilo usa um formato Huffman+LZ77 customizado implementado em `jq.java`, não DEFLATE standard
- **Solução**: Implementámos `HuffmanInflator.java` com bit reader de 32-bit words e árvore binária

### Reciclar LZ77 do iSiloDecompress
- **Tentativa**: Usar `iSiloDecompress.java` (implementação LZ77 antiga) para registos iSilo 3
- **Resultado**: Formato LZ77 errado — o iSilo usa um formato Huffman+LZ77 que não corresponde a nenhum standard
- **Solução**: `iSiloDecompress.java` deixou de ser usado; `HuffmanInflator` trata ambos os casos

### Cópia do JqDecompressor.kt (Kotlin)
- **Tentativa**: Copiar a implementação do projecto Kotlin
- **Resultado**: O utilizador informou que o projecto Kotlin está "incompleto e falho" — tem bugs e decisões erradas
- **Solução**: Baseámo-nos exclusivamente no código original decompilado (`jq.java`, `ny.java`, `no.java`)

### Offset de recordCount (72-73 vs 74-75 vs 76-77)
- **Tentativa (1)**: `recordCount = header[72..73]` — formato PalmDB original
- **Tentativa (2)**: `recordCount = header[74..75]` — padrão PalmDB standard
- **Resultado**: Nenhum funcionou. O `jt.java` original decompilado mostra `f(76)` = recordCount em offset 76-77
- **Solução**: `recordCount = header[76..77]` — confirmado pelo original decompilado

### TYPE/creator incorrectos
- **Tentativa**: Usar TYPE="SILO"/"SilX" e CREATOR="REAd" para identificar iSilo
- **Resultado**: Os ficheiros iSilo reais têm TYPE="SDoc" e CREATOR="SilX"
- **Solução**: Documentámos os valores reais: SDoc/SilX, ToGo/vários, TEXt/vários

### Page Tree — Travessia Completa
- **Tentativa**: Implementar travessia da page tree binária para extrair offsets (método `ny.java.a(eb, nz, int, int)`)
- **Resultado**: Método decompilado incorretamente pelo JADX (código bytecode raw, não Java)
- **Solução**: Implementámos decodificador de deltas baseado na análise do `a(nz)` que lê `eb.f(6)`, `eb.d(2)`, e itera sobre entradas com os 4 tipos de encoding

### Links (registos sub=0x01)
- **Tentativa**: Parsear registos type=0x04 sub=0x01 como pares (charOffset, targetOffset, linkLen, titleLen)
- **Resultado**: Formato diferente do assumido — bytes não correspondem à estrutura esperada
- **Solução**: Ainda não resolvido. Adicionámos dump hex dos registos para análise futura

### Imagens (grupos A[1..3])
- **Tentativa**: Não iniciado
- **Resultado**: N/A — os grupos A[1..3] têm count=0 nos ficheiros testados

---

## 3. O QUE FALTA FAZER (⬜)

### Prioridade Alta

| Tarefa | Descrição | Depende de |
|--------|-----------|------------|
| **Links** | Entender formato dos registos sub=0x01 e implementar navegação por links | Nada |
| **Formatação por bloco** | Extrair estilos (bold, italic, cores) dos flag bytes de cada bloco no texto comprimido | Nada |
| **TOC real** | Se k[1]/B[1] existirem, extrair entradas TOC e navegação | Nada |
| **Lazy loading refinado** | Usar pageOffsets para saltar directamente para qualquer página sem descomprimir tudo | Page offsets (parcialmente feito) |

### Prioridade Média

| Tarefa | Descrição |
|--------|-----------|
| **Imagens** | Extrair e renderizar imagens dos grupos A[1..3] |
| **Cores de texto/fundo** | Aplicar cores de texto e fundo dos estilos de bloco |
| **Pesquisa** | FindActivity actualmente funcional mas básica (case-sensitive) |
| **Marcadores/Bookmarks** | Adicionar suporte para bookmarks |
| **Anotações** | Adicionar suporte para anotações inline |

### Prioridade Baixa

| Tarefa | Descrição |
|--------|-----------|
| **Optimização** | Cache LRU de páginas descomprimidas |
| **Scroll contínuo** | Modo de scroll contínuo (actualmente só modo página) |
| **Impressão** | Suporte a impressão do documento |
| **Exportar texto** | Salvar como TXT |
| **Abrir por categoria** | DocListActivity agrupar por categorias |
| **Tela cheia** | Modo leitura sem distracções |
| **Ajuste de fonte** | Configuração de tamanho/face da fonte pelo utilizador |
| **Temas** | Modo nocturno, sépia, etc. |

---

## 4. DECISÕES TÉCNICAS

### Porque não usámos libiSilo.so (JNI)
- O dispositivo do utilizador só aceita 64-bit
- A `libiSilo.so` original é 32-bit (ARM)
- Reimplementámos em Java puro

### Porque não seguimos o projecto Kotlin
- O utilizador afirmou que o projecto Kotlin está "incompleto e falho"
- Algumas implementações (como a descompressão) têm bugs
- Usámos apenas como referência para entender a estrutura dos registos

### Porque o layout é incremental (chunks)
- Layout total O(n) para 15000 chars é rápido (~50ms)
- Para 2000+ páginas, layout total poderia levar segundos → ANR
- Chunks de 2500 caracteres distribuem o trabalho pela navegação

### Porque o encoding é detectado heuristicamente
- O campo encodingFlags (f(38)) no header é 0 para o ficheiro testado
- O encoding real vem dos dados de formato dos blocos (não do header)
- Usamos detectUTF8() como fallback: se vir sequências multi-byte válidas, assume UTF-8

---

## 5. FICHEIROS DO PROJECTO

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
│   ├── PalmDBImpl.java           (leitor PDB)
│   ├── LinkEntry.java            (dados de link)
├── render/
│   ├── CharMeasurer.java         (interface)
│   ├── AndroidCharMeasurer.java  (Paint + cache Typeface)
│   ├── TextStyle.java            (fontSize, bold, italic, typeface...)
│   ├── TextRun.java              (char[] + offset + length + style)
│   ├── FormattedText.java        (lista de TextRun[])
│   ├── LayoutEngine.java         (word wrap)
│   ├── LayoutLine.java           (linha com offset/y/width)
│   ├── Page.java                 (página com linhas)
│   ├── DocumentLayout.java       (layout incremental O(n))
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
│   ├── ReadActivity.java         (Activity + menu TOC/Procurar/Copiar)
│   ├── ReadView.java             (View + onDraw + links + navegação)
│   └── FindActivity.java         (diálogo de pesquisa)
├── doclist/
│   └── DocListActivity.java      (lista de .pdb)
└── iSiloActivityGroup.java       (deprecated)

docs/
├── architecture.md              (arquitectura detalhada)
└── summary.md                   (este ficheiro)
```
