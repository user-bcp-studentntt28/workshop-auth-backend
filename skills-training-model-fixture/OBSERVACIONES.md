# Observaciones de seguridad — Skills Training Model fixture

## Alcance y validación

- **Objetivo:** `skills-training-model-fixture/pom.xml` únicamente.
- **Inspección:** POM y archivos presentes en la fixture. No se revisó el POM de producción ni otros modelos de entrenamiento.
- **Resolución de dependencias:** `mvn -f .\skills-training-model-fixture\pom.xml dependency:tree` (Maven 3.9.16), ejecutado desde la raíz del backend. Resultado: `BUILD SUCCESS`; las dos dependencias aparecen como dependencias directas de alcance `compile`.
- **Escáner de vulnerabilidades:** no se ejecutó Fortify, Xray ni otro escáner. Las observaciones se basan en los avisos públicos citados, el POM y el árbol de dependencias; no se afirma que un escáner haya producido estos resultados.
- **Uso en código:** esta fixture solo contiene `pom.xml` y `README.md`; no contiene código de aplicación que utilice estas bibliotecas ni demuestre explotabilidad.

## Hallazgos

### 1. SnakeYAML 1.26 — CVE-2022-1471

- **Estado:** confirmado para la versión declarada; no se demostró explotación en esta fixture.
- **Severidad:** crítica según NVD (CVSS 9.8).
- **Evidencia y ruta:** `skills-training-model-fixture/pom.xml` declara `org.yaml:snakeyaml:1.26` directamente. El árbol resuelto muestra `skills-training-model-fixture -> org.yaml:snakeyaml:1.26` (`compile`).
- **Dónde se usa:** no hay código fuente en la fixture que invoque SnakeYAML o deserialice YAML.
- **Riesgo:** la deserialización insegura mediante `Constructor` al procesar YAML no confiable puede permitir ejecución de código.
- **Remediación fuera del ejercicio:** si la dependencia se necesita, actualizar a SnakeYAML 2.0 o posterior y comprobar compatibilidad; si no se necesita, quitar la declaración. No excluir clases: la dependencia es directa y las exclusiones Maven aplican a dependencias transitivas, además de no ser una corrección fiable para esta vulnerabilidad.
- **Fuente:** [NVD — CVE-2022-1471](https://nvd.nist.gov/vuln/detail/CVE-2022-1471).

### 2. Apache Commons Collections 3.2.1 — CVE-2015-6420

- **Estado:** versión afectada según el advisory de GitHub para el artefacto Maven; no se demostró explotación en esta fixture.
- **Severidad:** alta según Apache Commons Collections y el advisory de GitHub.
- **Evidencia y ruta:** `skills-training-model-fixture/pom.xml` declara `commons-collections:commons-collections:3.2.1` directamente. El árbol resuelto muestra `skills-training-model-fixture -> commons-collections:commons-collections:3.2.1` (`compile`).
- **Dónde se usa:** no hay código fuente en la fixture que utilice Commons Collections ni invoque deserialización.
- **Riesgo:** las versiones anteriores a 3.2.2 están asociadas con riesgo de deserialización insegura en el advisory consultado.
- **Nota sobre el identificador:** Apache señala que no se asignó un CVE directamente a Commons Collections; CVE-2015-6420 describe productos Cisco. El mapeo del artefacto Maven y la versión afectada se sustenta aquí en GHSA-6hgm-866r-3cjv y en el aviso de seguridad de Apache, no en atribuir a esta fixture un hallazgo de producto Cisco.
- **Remediación fuera del ejercicio:** si la dependencia se necesita, actualizar a 3.2.2 o posterior y comprobar compatibilidad; si no se necesita, quitar la declaración. No excluir clases: es una dependencia directa y una exclusión de clases no resuelve de forma fiable el riesgo de deserialización.
- **Fuentes:** [GitHub Advisory GHSA-6hgm-866r-3cjv](https://github.com/advisories/GHSA-6hgm-866r-3cjv); [Apache Commons Collections — Security](https://commons.apache.org/proper/commons-collections/security.html).

## Decisión para la fixture de entrenamiento

Estas versiones vulnerables son intencionales para comprobar que las herramientas de análisis detecten los componentes. Mantenerlas solo en esta fixture aislada durante el ejercicio; no agregarla al build de producción ni suprimir sus hallazgos. Al terminar el entrenamiento, eliminar la fixture o reemplazar las versiones por versiones corregidas. No se modificaron dependencias ni se aplicaron remediaciones.
