# Sample "Visual Flow" Diagram

The **Visual Flow** button now renders each checked alarm as a **vertical, centered flow** with a gray header block and stacked recipient stages. Arrows run downward between blocks and carry the configured delay, while each stage shows the time, an immediate/delay qualifier, and the recipient name.

## Example Layout
- Config Group: `Acute Care NC`
- Alarm Name: `Code Blue`
- Priority: `High Priority`
- Stage 1: `0 sec` to `Primary RN` (Immediate)
- Stage 2: `60 sec` to `Charge RN`

## PlantUML snippet
The snippet below matches the styling used by the in-app export (rounded blocks, green first stage, blue second stage, and arrow labels for delays):

```plantuml
@startuml
top to bottom direction
skinparam shadowing false
skinparam backgroundColor #FFFFFF
skinparam rectangle {
  RoundCorner 16
  FontSize 14
  FontColor #111111
}
skinparam rectangle<<Header>> { BackgroundColor #f2f2f2 BorderColor #9a9a9a }
skinparam rectangle<<Stage1>> { BackgroundColor #c8f7c5 BorderColor #4f9a4f }
skinparam rectangle<<Stage2>> { BackgroundColor #cfe2ff BorderColor #4a78c2 }
skinparam rectangle<<StageTail>> { BackgroundColor #cfe2ff BorderColor #4a78c2 }
skinparam ArrowColor #333333
skinparam ArrowFontSize 12
skinparam ArrowThickness 1.4

rectangle "Acute Care NC\nCode Blue\nHigh Priority" as Header_1 <<Header>>
rectangle "0 sec\n(Immediate)\nRecipient:\nPrimary RN" as Stage_1_1 <<Stage1>>
Header_1 -down-> Stage_1_1

rectangle "60 sec\n(60 sec)\nRecipient:\nCharge RN" as Stage_1_2 <<Stage2>>
Stage_1_1 -down-> Stage_1_2 : 60 sec
@enduml
```

To preview locally (without committing binaries):
1. Download PlantUML 1.2023.13: `curl -L -o /tmp/plantuml.jar https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/1.2023.13/plantuml-1.2023.13.jar`
2. Render PNG: `java -jar /tmp/plantuml.jar -tpng docs/view-flow-sample.puml -o .`
3. Render PDF: `java -jar /tmp/plantuml.jar -tpdf docs/view-flow-sample.puml -o .`
