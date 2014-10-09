set SPOON_NAMESPACE={namespace}
set SPOON_REPO={repo-name}
set VERSION={version}
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
  rem This is to avoid filling the build machine will useless images
  spoon rmi --all
) ELSE (
  spoon push %SPOON_NAMESPACE%/%SPOON_REPO%:%VERSION%
  rem This is to avoid filling the build machine will useless images
  spoon rmi --all
)
