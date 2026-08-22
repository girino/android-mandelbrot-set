# Postmortem — viewport, flicker e gestos (pan/pinch)

Documento de handoff para continuidade do trabalho no app **Fractal FOSS**  
(`org.girino.frac.android.foss`). Atualizado: **2026-08-22**.

---

## Resumo executivo

| Item | Estado |
|------|--------|
| **Problema original** | Salto/flicker de viewport em pan/pinch ([#2](https://github.com/girino/android-mandelbrot-set/issues/2)) |
| **Baseline que funcionava “melhor”** | Tag **`v1.0.0`** (~280 linhas, `ScaleGestureDetector`, preview simples) |
| **Último commit git (`HEAD`)** | ~613 linhas — pinch manual, bridge afim, `awaitingBitmapPublish`, etc. |
| **Working tree (não commitado)** | **Rollback para v1.0.0** + hooks de teste (~271 linhas) |
| **Bug resolvido?** | **Não** — usuário confirmou flicker persistente após várias abordagens |
| **Política do usuário** | **Não alterar código sem pedido explícito**; perguntas → responder, não implementar |

---

## Sintomas reportados pelo usuário

1. **Salto/flicker** ao soltar pinch ou pan — bitmap aparece brevemente na posição errada e “volta”.
2. Cenários problemáticos:
   - Pinch + arrasto (dois dedos)
   - Pan → pinch (um dedo, depois segundo dedo)
   - Zoom out + movimento
   - Gestos rápidos encadeados antes do render terminar
3. **v1.0.0 funcionava melhor** que todas as iterações posteriores (feedback explícito após horas de tentativas).
4. Após tentativas de fix de flicker na sessão Cursor:
   - Pausa total de render durante gesto → **não resolveu**
   - Bloqueio de publish com `hasLivePreview()` → **piorou** (preview de pinch sem escala + flicker continuou)
5. Usuário pediu **revert** e investigação **sem alterar código** até nova decisão.

---

## Baseline v1.0.0 (referência)

Arquivo: `app/src/main/java/org/girino/frac/android/foss/MandelbrotView.java` (tag `v1.0.0`).

### Gestos

- **`ScaleGestureDetector`** nativo (não pinch manual).
- **Preview** em `onDraw`:
  ```java
  float dx = (1 - accumulatedScale) * width / 2f;
  float dy = (1 - accumulatedScale) * height / 2f;
  canvas.translate(positionX + dx, positionY + dy);
  canvas.scale(accumulatedScale, accumulatedScale);  // centro da tela, não foco
  canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
  ```
- **Pan (1 dedo):** acumula `positionX/Y`; commit no `ACTION_UP`: `center -= position/scale`.
- **Pinch:** `onScaleBegin` → `stop()`; `onScale` → `accumulatedScale *= factor`; `onScaleEnd` → `scale *= accumulatedScale`, `start()`.
- **Pan durante pinch:** bloqueado (`!scaleDetector.isInProgress()`).

### Render

- Thread única, loop progressivo **step 8 → 4 → 2 → 1**.
- **A cada step:** `post(() -> { bitmap = rendered; invalidate(); })`.
- **`renderGeneration`** cancela renders obsoletos via `stop()`.

### Limitações conhecidas da v1.0.0

- Pinch escala em torno do **centro da tela**, não do foco entre os dedos (issue #2).
- **`onScaleBegin` chama `stop()`**, mas **pan com 1 dedo não** — render continua durante pan.
- **`onScaleEnd` chama `start()`** antes do `ACTION_UP` — pan pendente só commita no UP (ordem pan→pinch).

---

## Causa provável do flicker (consenso da investigação)

O preview é **só transformação de canvas**. A troca que não é preview:

```java
// Dentro de render(), repetido para steps 8, 4, 2, 1:
post(() -> {
    bitmap = rendered;  // ← substitui textura base
    invalidate();
});
```

Durante drag/pinch, o render **anterior** (viewport antigo) pode ainda publicar frames coarse (step=8…) **enquanto** `onDraw` aplica preview. A textura base muda de repente → **flash/flicker**.

No histórico do repo, correções tentaram:

1. Manter preview até bitmap novo (handoff no step=8 ou step=1)
2. Cancelar render no início do gesto
3. Não publicar bitmap enquanto preview ativo
4. Sincronizar viewport lógico vs bitmap publicado (`publishedCenter*`, bridge afim)

Nenhuma abordagem validada como **resolvida no dispositivo** pelo usuário.

---

## Timeline de desenvolvimento

### 2026-08-14 — v1.0.0

- Release estável FOSS; gestos simples descritos acima.

### 2026-08-22 — Série de commits (entre `v1.0.0` e `HEAD`)

| Commit | Intenção | Resultado / problema |
|--------|----------|----------------------|
| `e278d3c` | Fix #2: zoom no foco; extrair `ViewportTransforms`; testes JVM | Início da complexidade; preview no foco |
| `2ec2bc2` / `31ba4e0` | Release 1.0.1-alpha / 1.0.1 | Builds para teste |
| `ee936df` | Adiar render até fim do gesto | Ainda saltos |
| `5042cb6` | Adiar commit center/scale até publish do bitmap | Viewport “pendente” |
| `19a1ace` | Manter preview até render full-res | Preview congelado / timing |
| `0b69c86` | Evitar double-count de commit | Regressões de coordenadas |
| `acd2791` | Corrida render vs gesto; regra smooth transition | `pendingViewportGeneration` |
| `64b6d33` | Handoff preview → bitmap no **step=8** | Requisito de produto formalizado |
| `5f01390` | Fix handoff quando step=8 adiado/descartado | Último estado commitado (~613 linhas) |

Commits paralelos (Zapstore, regras git, lint) — não afetam viewport.

### 2026-08-22 — Sessão Cursor (não commitada)

Transcript: `agent-transcripts/7a91e6c9-1c44-4235-a39d-a2b4d47973ed.jsonl`

| Fase | O que foi feito | Feedback do usuário |
|------|-----------------|---------------------|
| 1 | Rollback radical para v1.0.0 | “Funcionava melhor”, mas bug persiste |
| 2 | **Pausa de render:** `beginGesture`/`endGestureAndRender`, `gestureInProgress`, bloqueio publish | Instalado; **não resolveu** flicker |
| 3 | Testes de regressão da pausa | 83–84 testes passando |
| 4 | Usuário pediu revert + investigar bitmap durante drag | — |
| 5 | **`hasLivePreview()`** bloqueia publish no `post()` do render | Preview pinch sem escala; flicker continua; **revert pedido** |
| 6 | **Estado atual:** v1.0.0 puro + hooks mínimos de teste | **Não alterar código sem pedir** |

---

## Abordagens tentadas — detalhe do que deu errado

### A. Preview no foco + `ViewportTransforms.commitPinch` (pós-v1.0.0, commits)

**Ideia:** Corrigir issue #2 ancorando zoom no foco do gesto.

**Problemas:**
- Preview (`scale` no foco) e commit (fórmula de centro) **divergiam** → salto ao soltar.
- Preview mantido até bitmap novo → flash na posição original se timing errado.

### B. Viewport pendente / `awaitingBitmapPublish` / `publishedCenter*`

**Ideia:** Viewport lógico commitado cedo; bitmap publicado depois; bridge afim entre publicado e alvo.

**Problemas:**
- Bridge `scale-about-center` + commit por pivô **inconsistentes** em zoom out + movimento.
- Bitmap aparecia na posição antiga e depois “volta”.
- Complexidade (~600 linhas) difícil de raciocinar.

### C. Pinch manual (dois dedos) em vez de `ScaleGestureDetector`

**Ideia:** Controle total de span, midpoint, pan acumulado.

**Problemas:**
- `beginPinchSession` resetava/dobrava pan → pan ignorado ou flash ao segundo dedo.
- Divergia do comportamento nativo que o usuário preferia na v1.0.0.

### D. Handoff preview → bitmap no step=8 (regra `.cursor/rules/viewport-smooth-transition.mdc`)

**Ideia:** Ao soltar, publicar bitmap coarse correto e **só então** remover preview.

**Problemas:**
- Corrida se novo gesto antes do render terminar.
- Step=8 adiado/descartado quebrava handoff (`5f01390` tentou corrigir).
- Usuário ainda reportou flicker/saltos.

### E. Pausa total de render durante gesto (sessão Cursor)

**Ideia:** `ACTION_DOWN` / `onScaleBegin` → `stop()` + `gestureInProgress`; `UP` → `start()`.

**Problemas:**
- Usuário: **não resolveu** no aparelho.
- `onScaleEnd` sem `start()` + `UP` com `endGestureAndRender` — ordem pan/pinch frágil.

### F. Bloquear publish se `hasLivePreview()` (sessão Cursor)

**Ideia:** Render continua, mas `bitmap = rendered` só quando preview idle.

**Problemas (reportados):**
- Preview de pinch **não mudava de tamanho** (regressão perceptível).
- Flicker **continuou**.
- Revertido.

---

## Estado do código agora

### Git `HEAD` (commit `5f01390`) — **não** é o que está no working tree

`MandelbrotView.java` ~613 linhas, inclui entre outros:

- Pinch manual / sessão de pinch
- `previewScale`, `previewPosX/Y`, `previewFocusX/Y`
- `sessionBasePreview*`
- `publishedCenterX/Y`, `publishedScale`
- `awaitingBitmapPublish`, `pendingViewportGeneration`, `previewHandoffPending`
- `ViewportTransforms.PreviewBridge` / `bridgeFromPublishedToTarget`
- Debug viewport (`Log`, `debugViewport`)

### Working tree (2026-08-22 fim da sessão)

**Rollback para v1.0.0** + hooks Robolectric:

```text
M  app/src/main/java/.../MandelbrotView.java     (~271 linhas, v1.0.0)
M  app/build.gradle.kts                          (+ Robolectric, JUnit vintage)
M  app/src/main/java/.../ViewportTransforms.java (+ PreviewBridge etc.)
M  app/src/test/java/.../ViewportTransformsTest.java
??  app/src/test/java/.../android/foss/          (gesture tests + simulator)
??  scripts/setup-*.ps1
??  .cursor/rules/headless-gesture-tests.mdc
M  .cursor/rules/local-android-sdk.mdc
```

**Importante:** `ViewportTransforms` e testes de math ainda refletem abordagem foco/bridge; **`MandelbrotView` v1.0.0 não usa `ViewportTransforms`**.

---

## Infraestrutura útil (manter)

### Testes headless (Robolectric)

| Arquivo | Função |
|---------|--------|
| `app/src/test/.../MandelbrotViewGestureTest.java` | Pan + pinch básicos (2 testes no rollback) |
| `app/src/test/.../PinchDragMotionSimulator.java` | Injeta `MotionEvent` |
| `app/src/test/.../ViewportTransformsTest.java` | 11+ testes de math pura |
| `app/build.gradle.kts` | `robolectric:4.14.1`, JUnit 5 + vintage |

```powershell
$env:JAVA_HOME = Join-Path $PWD ".jdk"
$env:ANDROID_HOME = Join-Path $PWD ".android-sdk"
.\gradlew.bat testDebugUnitTest
# ou
.\scripts\setup-headless-tests.ps1 -RunTests
```

**Limitação:** `ScaleGestureDetector` no Robolectric **não reproduz** pinch fielmente; testes de gesto completo são fracos. Math em `ViewportTransformsTest` é confiável.

### SDK/JDK locais

Regra: `.cursor/rules/local-android-sdk.mdc`

| Pasta | Conteúdo |
|-------|----------|
| `.jdk/` | Temurin 17 |
| `.android-sdk/` | Platform 36, Build-Tools 36.0.0, platform-tools (adb) |
| `local.properties` | `sdk.dir=.android-sdk` (gitignored) |

Scripts:

- `scripts/setup-android-sdk.ps1` — instala platform-tools, platform, build-tools
- `scripts/setup-headless-tests.ps1` — verifica env + testes
- `scripts/setup-android-emulator.ps1` — emulador opcional

### Regras Cursor (`.cursor/rules/`)

| Regra | `alwaysApply` | Notas |
|-------|---------------|-------|
| `viewport-smooth-transition.mdc` | **sim** | Handoff step=8 — **não validado** pelo usuário |
| `local-android-sdk.mdc` | sim | SDK/JDK locais |
| `git-commit-policy.mdc` | sim | Conflita com user rule “commit só se pedir” — **preferir pedido do usuário** |
| `headless-gesture-tests.mdc` | glob | **não commitada** |
| `zapstore-wsl.mdc` | — | Publicação Zapstore |

### Dispositivo de teste

- Serial visto: **`Q4INYDJRZ9BEPFCY`**
- Instalar: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Debug `versionName`: `1.0.1-<timestamp>` (suffix em debug builds)

---

## O que NÃO repetir

1. **Reescrita grande** de `MandelbrotView` sem validação incremental no aparelho.
2. **Pinch manual** + bridge afim + viewport pendente **de uma vez**.
3. **Preview no foco** sem provar que commit e `onDraw` usam **exatamente** o mesmo modelo.
4. **`hasLivePreview()` bloqueando publish** — testado; usuário rejeitou.
5. **Pausa total de render no DOWN** — testado; usuário disse que não resolveu.
6. **Assumir que testes Robolectric** cobrem flicker visual.
7. **Alterar código** quando o usuário fez **pergunta** ou pediu **análise**.
8. **Commit/push automático** sem pedido (frustração explícita).

---

## Hipóteses abertas para o próximo agente

1. **Flicker durante drag (v1.0.0):** publish progressivo (steps 8→1) com viewport **stale** durante pan — pan não chama `stop()` no DOWN.
2. **Flicker ao soltar:** preview zera / `accumulatedScale=1` **antes** do bitmap novo nas coordenadas corretas (`onScaleEnd` → `start()` imediato na v1.0.0).
3. **Pan→pinch:** pan em `positionX/Y` commitado só no UP, mas scale commitado no `onScaleEnd` — ordem inconsistente.
4. **Regra step=8** pode ser correta em produto, mas implementação em `HEAD` estava over-engineered.

### Abordagens a discutir **com o usuário antes de codar**

| Abordagem | Escopo | Risco |
|-----------|--------|-------|
| `stop()` no **primeiro MOVE** de pan (ou DOWN) | 1 linha + teste manual | Baixo |
| Manter preview até **step=8 do render novo** (mínimo, sem bridge) | Pequeno diff sobre v1.0.0 | Médio — timing |
| Zoom no foco via `ScaleGestureDetector.getFocusX/Y` + commit consistente | Médio | Médio — regressão #2 |
| Instrumentação: log/contador de publish durante gesto | Debug only | Nenhum |

---

## Comandos rápidos

```powershell
Set-Location F:\cygwin64\home\girino\git\android-mandelbrot-set
$env:JAVA_HOME = Join-Path $PWD ".jdk"
$env:ANDROID_HOME = Join-Path $PWD ".android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

Comparar com baseline:

```powershell
git show v1.0.0:app/src/main/java/org/girino/frac/android/foss/MandelbrotView.java
git diff v1.0.0 -- app/src/main/java/org/girino/frac/android/foss/MandelbrotView.java
git show HEAD:app/src/main/java/org/girino/frac/android/foss/MandelbrotView.java  # versão complexa commitada
```

---

## Referências

- **Issue GitHub:** [#2 Pinch zoom causes abrupt viewport movement](https://github.com/girino/android-mandelbrot-set/issues/2)
- **Tag baseline:** `v1.0.0`
- **Último commit branch:** `5f01390`
- **Transcript sessão principal:** `7a91e6c9-1c44-4235-a39d-a2b4d47973ed` (Cursor agent transcripts)
- **Arquivo central:** `app/src/main/java/org/girino/frac/android/foss/MandelbrotView.java`
- **Math extraída:** `app/src/main/java/org/girino/frac/viewport/ViewportTransforms.java`

---

## Checklist para retomada

- [ ] Confirmar com usuário: baseline desejado é **v1.0.0 working tree** ou **HEAD** (`5f01390`)?
- [ ] Reproduzir flicker com passos exatos (gesto, zoom in/out, timing).
- [ ] Decidir **uma** hipótese; validar no aparelho antes da próxima.
- [ ] Não reintroduzir bridge/pinch manual sem necessidade comprovada.
- [ ] Commitar apenas se usuário pedir (scripts/regras ainda não commitados).
- [ ] Atualizar este postmortem ao fechar uma abordagem (sucesso ou falha).
