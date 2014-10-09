set SPOON_NAMESPACE={namespace}
set SPOON_REPO={repo-name}
set VERSION={version}
echo Will build file %SPOONFILE%
echo Version selected %VERSION%
if NOT DEFINED SPOONFILE (
	set SPOONFILE=spoon.me
)
if NOT DEFINED VERSION (
	echo "VERSION is missing"
	exit 1
)
spoon logout
spoon login %SPOON_USER% %SPOON_PWD%
spoon config --hub=http://dev-stage.spoon.net
spoon build %SPOON_BUILD_FLAVOR% --overwrite --name=%SPOON_NAMESPACE%/%SPOON_REPO%:%VERSION% %SPOONFILE%

IF NOT '%ERRORLEVEL%'=='0' (
  echo "Failed build with %ERRORLEVEL%"
  exit %ERRORLEVEL%
)

IF EXIST test (
  echo "Rough test before publishing"
  cd test/

  set TO_TEST=%SPOON_NAMESPACE%/%SPOON_REPO%:%VERSION%
  test-script.bat

  IF NOT '%ERRORLEVEL%'=='0' (
    echo "Failed test with %ERRORLEVEL%"
    exit %ERRORLEVEL%
  )
  spoon push %SPOON_NAMESPACE%/%SPOON_REPO%:%VERSION%
  if %ERRORLEVEL% neq 0 exit %ERRORLEVEL%
  rem This is to avoid filling the build machine will useless images
  spoon rmi --all
) ELSE (
  spoon push %SPOON_NAMESPACE%/%SPOON_REPO%:%VERSION%
  if %ERRORLEVEL% neq 0 exit %ERRORLEVEL%
  rem This is to avoid filling the build machine will useless images
  spoon rmi --all
)
