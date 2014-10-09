set SPOON_NAMESPACE={namespace}
set SPOON_REPO={repo-name}
set VERSION={version}
IF EXIST test (
  spoon logout
  spoon login %SPOON_USER% %SPOON_PWD%
  spoon config --hub=http://dev-stage.spoon.net
  echo "Rough test before publishing"
  cd test/

  set TO_TEST=%SPOON_NAMESPACE%/%SPOON_REPO%:%VERSION%
  test-script.bat

  IF NOT '%ERRORLEVEL%'=='0' (
    echo "Failed test with %ERRORLEVEL%"
    exit %ERRORLEVEL%
  )
) ELSE (
   echo "no tests. we're done"
)
