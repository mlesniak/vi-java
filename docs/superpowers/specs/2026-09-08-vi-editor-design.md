# Design: Minimalistischer vi-Editor in Java

Datum: 2026-09-08

## Ziel

Ein lauffähiger, minimaler vi-Klon als Lernprojekt. Plain Java 25, Maven, keine
Laufzeit-Dependencies. Produktivcode unter 1024 Zeilen, sauber formatiert (kein
Golfen, keine Mehrfach-Statements pro Zeile).

## Nicht-Ziele

- Windows-Unterstützung (Raw-Mode läuft über `stty`, also nur Linux/macOS).
- Syntax-Highlighting, Suche, Zähler-Präfixe, Register außer einem Yank-Register,
  Visual-Mode, Konfiguration, mehrere Buffer.
- Fensterresize zur Laufzeit (Größe wird beim Start einmal abgefragt).

## Funktionsumfang

### Modi

- NORMAL (Start), INSERT, COMMAND (nach `:`).
- `Esc` verlässt INSERT und COMMAND zurück nach NORMAL.

### NORMAL-Mode Tasten

| Taste | Wirkung |
|---|---|
| `h` `j` `k` `l`, Pfeiltasten | Cursor bewegen, Grenzen werden geklemmt |
| `0` | Zeilenanfang |
| `$` | Zeilenende (letztes Zeichen) |
| `w` | Nächster Wortanfang (Wort = Folge von Nicht-Whitespace), über Zeilengrenzen hinweg |
| `b` | Vorheriger Wortanfang, über Zeilengrenzen hinweg |
| `gg` | Erste Zeile |
| `G` | Letzte Zeile |
| `i` | INSERT vor Cursor |
| `a` | INSERT nach Cursor |
| `o` | Neue Zeile unterhalb, INSERT |
| `O` | Neue Zeile oberhalb, INSERT |
| `x` | Zeichen unter Cursor löschen |
| `dd` | Zeile löschen, in Register |
| `yy` | Zeile in Register kopieren |
| `p` | Register als neue Zeile unterhalb einfügen |
| `u` | Undo (letzter Snapshot) |
| `:` | COMMAND-Mode |

Pending-Tasten (`d`, `y`, `g`) warten auf zweite Taste. Jede andere zweite Taste
bricht ab. Unbekannte Tasten werden ignoriert.

Der Cursor in NORMAL steht maximal auf dem letzten Zeichen der Zeile (bei leerer
Zeile auf Spalte 0). Bei vertikaler Bewegung wird die Spalte geklemmt, nicht
gemerkt (kein "sticky column"; bewusst weggelassen).

### INSERT-Mode Tasten

- Druckbare Zeichen (32..126): einfügen an Cursor, Cursor nach rechts.
- `Enter` (13): Zeile am Cursor teilen.
- `Backspace` (127 oder 8): Zeichen links löschen; an Spalte 0 mit Vorzeile
  verbinden.
- Pfeiltasten: bewegen. Cursor darf im INSERT auf Länge der Zeile stehen.
- `Esc`: zurück nach NORMAL, Cursor eine Spalte nach links (vi-Verhalten), falls
  nicht schon bei 0.

Ein kompletter INSERT-Aufenthalt ist ein Undo-Schritt (Snapshot beim Betreten).

### COMMAND-Mode

Eingabe wird in der untersten Zeile mit `:`-Präfix angezeigt. `Backspace` löscht,
bei leerem Text zurück nach NORMAL. `Enter` führt aus:

| Kommando | Wirkung |
|---|---|
| `w` | Speichern unter aktuellem Namen; Fehler `No file name` ohne Namen |
| `w NAME` | Speichern unter NAME, NAME wird aktueller Name |
| `q` | Beenden; Fehler `No write since last change (add ! to override)` wenn dirty |
| `q!` | Beenden ohne Speichern |
| `wq` | Speichern und beenden |
| `e NAME` | NAME laden (nur wenn nicht dirty, sonst gleiche Fehlermeldung wie `q`) |

Unbekannte Kommandos: Statusmeldung `Not an editor command: X`. IO-Fehler werden
als Statusmeldung angezeigt, nie als Exception nach oben gereicht.

### Anzeige

- Alternate Screen (`ESC[?1049h` / `ESC[?1049l`).
- Sichtbare Zeilen ab `scroll`-Offset, `rows - 1` Textzeilen. Fehlende Zeilen
  werden als `~` dargestellt. Zeilen länger als `cols` werden abgeschnitten
  (kein Wrapping, kein horizontales Scrollen; Cursor physisch auf `cols - 1`
  geklemmt).
- Letzte Zeile: im COMMAND-Mode die Kommandozeile, sonst Statuszeile:
  `NAME [+] -- INSERT --  row,col` (1-basiert) bzw. eine Statusmeldung, die bis
  zum nächsten Tastendruck sichtbar bleibt. Ohne Namen `[No Name]`.
- Scrolling: nach jedem Tastendruck wird `scroll` so angepasst, dass die
  Cursorzeile sichtbar ist.

## Architektur

Paket `com.mlesniak.vi`, fünf Klassen.

### `Terminal`

Kapselt alles Betriebssystemnahe. Einzige Klasse mit Seiteneffekten auf stdin/stdout.

- `void enableRaw() throws IOException`: sichert `stty -g`, setzt
  `stty raw -echo`, betritt Alternate Screen. Beide über
  `ProcessBuilder("stty", ...)` mit `redirectInput(new File("/dev/tty"))`.
- `void restore()`: Alternate Screen verlassen, `stty <gesichert>`.
- `int[] size()`: `stty size` liefert `rows cols`; Fallback `24 80`.
- `int readKey() throws IOException`: liest ein Byte aus `System.in`. Bei `27`:
  wenn `System.in.available() > 0`, liest `[` und Buchstabe, liefert
  `UP`/`DOWN`/`RIGHT`/`LEFT`. Sonst `27`. Bei EOF `-1`.
- `void write(String s)`: schreibt auf `System.out`, flush.

Keys sind `int`. Konstanten `ESC = 27`, `ENTER = 13`, `BACKSPACE = 127`,
`UP = 1000`, `DOWN = 1001`, `RIGHT = 1002`, `LEFT = 1003` liegen als
`static final int` in `Terminal`, damit `Editor` und Tests sie nutzen können,
ohne ein Terminal zu instanziieren.

### `Buffer`

Textmodell. Keine Cursor-Logik.

- Felder: `List<String> lines` (nie leer, mindestens `""`), `String name`
  (kann `null` sein), `boolean dirty`, `Deque<List<String>> undo`.
- `Buffer()`: leerer Buffer ohne Namen.
- `static Buffer load(String name) throws IOException`: liest Datei falls
  vorhanden, sonst leerer Buffer mit Namen.
- `void save() throws IOException`: schreibt `lines` mit `\n` getrennt und
  abschließendem `\n`, setzt `dirty = false`. Ohne Namen `IOException("No file name")`.
- `String line(int row)`, `int size()`, `String name()`, `void name(String)`,
  `boolean dirty()`.
- `void insert(int row, int col, char c)`, `void delete(int row, int col)`,
  `void split(int row, int col)`, `void join(int row)` (hängt `row+1` an `row`),
  `void insertLine(int row, String s)`, `String deleteLine(int row)`
  (Buffer bleibt nie leer: letzte Zeile löschen ergibt `""`).
- `void snapshot()`: legt `new ArrayList<>(lines)` auf `undo`.
  `boolean undo()`: holt Snapshot zurück, `false` wenn leer. Undo setzt
  `dirty = true`.

Jede mutierende Methode setzt `dirty = true`. Mutationen bauen neue Strings
(`substring` + Konkatenation); für ein Lernprojekt ausreichend.

### `Editor`

Reine Zustandsmaschine, kein IO außer `Buffer.load/save`.

- Felder: `Buffer buf`, `Mode mode` (enum NORMAL/INSERT/COMMAND), `int row, col`,
  `int scroll`, `int rows, cols`, `int pending` (0 = keine), `String register`
  (`null` = leer), `StringBuilder cmd`, `String status`, `boolean running = true`.
- `Editor(Buffer buf, int rows, int cols)`.
- `void handleKey(int key)`: löscht `status`, dispatcht nach `mode`, danach
  `clampCursor()` und `adjustScroll()`.
- `private void normal(int key)`, `insert(int key)`, `command(int key)`,
  `execute(String line)`: Switch über `int`.
- Wortbewegung `wordForward()` / `wordBackward()` als eigene Methoden.
- Getter für `Screen` und Tests: `mode()`, `row()`, `col()`, `scroll()`,
  `status()`, `cmd()`, `buffer()`, `running()`.

### `Screen`

- `static String render(Editor e)`: baut den kompletten Frame als String:
  Cursor verstecken (`ESC[?25l`), `ESC[H`, pro Zeile Text oder `~` plus
  `ESC[K` und `\r\n`, letzte Zeile Status/Kommando, Cursor positionieren
  (`ESC[row;colH`, 1-basiert), Cursor zeigen (`ESC[?25h`).
  Ein `write`-Aufruf pro Frame, kein Flackern.

### `Vi`

- `main(String[] args)`: `Buffer.load(args[0])` oder `new Buffer()`, `Terminal`,
  `Editor` mit `terminal.size()`. Loop `while (editor.running())`:
  `terminal.write(Screen.render(editor)); key = terminal.readKey(); if (key < 0) break; editor.handleKey(key)`.
  `try/finally` ruft `terminal.restore()`.

## Datenfluss

```
stdin -> Terminal.readKey -> Editor.handleKey -> Buffer (Mutation)
Editor + Buffer -> Screen.render -> Terminal.write -> stdout
```

## Fehlerbehandlung

- IO-Fehler in `:w`/`:e`: Statusmeldung mit `e.getMessage()`.
- Nicht lesbare Datei beim Start: Meldung auf stderr, Exit 1, vor Raw-Mode.
- `stty` nicht verfügbar: `enableRaw` wirft `IOException`, `main` gibt Meldung aus
  und beendet sich. Kein halb-initialisiertes Terminal.
- `finally` in `main` garantiert `restore()` auch bei Exceptions.

## Tests

JUnit 5 (`junit-jupiter`, Scope `test`), Surefire 3.x. Tests zählen nicht zum
Zeilenbudget.

- `BufferTest`: insert/delete/split/join/insertLine/deleteLine, nie leer,
  Snapshot/Undo, Load/Save über `@TempDir`.
- `EditorTest`: Hilfsmethode `type(String keys)`, die jede Taste an `handleKey`
  gibt (`Esc` als `\u001b`, Enter als `\r`). Prüft Zeilen, Cursor, Modus,
  Scroll, Status, `running`. Deckt jede Taste aus den Tabellen mit mindestens
  einem Fall ab, plus Randfälle (leerer Buffer, letzte Zeile, `dd` auf einziger
  Zeile, `q` bei dirty).
- `ScreenTest`: Rendering von `~`-Zeilen, Abschneiden langer Zeilen,
  Statuszeile, Cursorposition im Frame.

`Terminal` bleibt ungetestet (benötigt echtes TTY).

## Build und Start

- `pom.xml`: `maven.compiler.release` 25, `junit-jupiter` 5.x test, Surefire 3.x.
- `mvn -q package` und `java -cp target/classes com.mlesniak.vi.Vi datei.txt`.
- Alternativ ohne Maven: `java src/main/java/com/mlesniak/vi/Vi.java datei.txt`
  (Multi-File-Source-Launch ab Java 22).

## Stil und Zeilenbudget

- 4 Leerzeichen, max. 100 Zeichen pro Zeile, Klammern immer, ein Statement pro
  Zeile, keine Zeilenzusammenlegung zur Zeilenersparnis.
- Switch-Expressions und `var` erlaubt, wo sie lesbarer sind.
- Budget-Richtwerte: Terminal 80, Buffer 100, Editor 350, Screen 50, Vi 40.
  Messung: `cat src/main/java/com/mlesniak/vi/*.java | wc -l` unter 1024.
