# Sample "View Flow" Box Diagram

The **View Flow** button now produces a horizontal, timeline-style box diagram for each checked (in-scope) alarm. A header note shows the alert name, flow type, and priority, while the recipients are drawn as rounded rectangles connected left-to-right with their configured time offsets labeled on the links (and the initial offset above the first box).

Below is a single-flow example that matches the in-app export layout for the attached mockup:

- Alert: `Toilet Assist`
- Type: `NurseCalls`
- Priority: `Medium`
- Recipients: `Assistant, NST (0 sec)`, `Buddy Assistant (60 sec)`, `Nurse (60 sec)`, `Nurse Buddy (60 sec)`

## PlantUML snippet
The diagram is produced with the same structure used by the application:

```plantuml
@startuml
left to right direction
skinparam shadowing false
skinparam backgroundColor #FFFFFF
skinparam componentStyle rectangle
skinparam rectangle {
  RoundCorner 12
  BorderColor #000000
  FontSize 14
}
skinparam note {
  BackgroundColor #FFFFFF
  BorderColor #111111
  RoundCorner 10
  FontSize 14
}
skinparam ArrowColor #333333
skinparam ArrowFontSize 13

note "Alert: Toilet Assist\nType: NurseCalls\nPriority: Medium" as Header_1 #FFFFFF

rectangle "Assistant, NST" as Step_1_1 #FDF0E7
note top of Step_1_1 : 0 sec

rectangle "Buddy Assistant" as Step_1_2 #FDF0E7
rectangle "Nurse" as Step_1_3 #FDF0E7
rectangle "Nurse Buddy" as Step_1_4 #FDF0E7

Header_1 -[hidden]-> Step_1_1
Step_1_1 --> Step_1_2 : 60 sec
Step_1_2 --> Step_1_3 : 60 sec
Step_1_3 --> Step_1_4 : 60 sec
@enduml
```

To regenerate the diagram locally (requires Java and Graphviz) without committing binary assets:
1. Download PlantUML 1.2023.13: `curl -L -o /tmp/plantuml.jar https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/1.2023.13/plantuml-1.2023.13.jar`
2. Run: `java -jar /tmp/plantuml.jar -tpng docs/view-flow-sample.puml -o .` (produces `view-flow-sample.png`)
3. For a PDF version: `java -jar /tmp/plantuml.jar -tpdf docs/view-flow-sample.puml -o .` (produces `view-flow-sample.pdf`)
