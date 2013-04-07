if not exist SLaB mkdir SLaB
if not exist SLaB\Source mkdir SLaB\Source
if not exist SLaB\Binaries mkdir SLaB\Binaries
if not exist "SLaB\Build Utils" mkdir "SLaB\Build Utils"
robocopy ..\Source SLaB\Source * /S /XD Tools bin ClientBin obj *Resharper* .svn /XF *.suo *Private.snk *.vssscc *.vspscc *ReSharper*
copy ..\Source\Bin\Release\* SLaB\Binaries
copy "..\Build Utils\*.exe" "SLaB\Build Utils"
del "SLaB\Build Utils\*.vshost.exe"
copy OtherFiles\* SLaB
attrib -R SLaB\* /S
PostProcessor
