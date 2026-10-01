#define AppName "Dima AI"
#define AppVersion "0.0.3"
#define AppExe "DimaAI.exe"

[Setup]
AppId={{B9C3227D-76B1-4A20-A523-45917F89F85A}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=NotHIGH
DefaultDirName={autopf}\Dima AI
DefaultGroupName=Dima AI
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=lowest
OutputDir=publish\installer
OutputBaseFilename=DimaAI-Setup-{#AppVersion}-Windows-x64
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\{#AppExe}

[Languages]
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"

[Tasks]
Name: "desktopicon"; Description: "Создать ярлык на рабочем столе"; GroupDescription: "Дополнительные ярлыки:"; Flags: unchecked

[Files]
Source: "publish\win-x64\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Dima AI"; Filename: "{app}\{#AppExe}"
Name: "{autodesktop}\Dima AI"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "Запустить Dima AI"; Flags: postinstall nowait skipifsilent