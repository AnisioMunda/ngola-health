package ao.hospitalao.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.HashSet;
import java.util.Set;

@AnalyzeClasses(packages = "ao.hospitalao.modules", importOptions = DoNotIncludeTests.class)
class ModuleArchitectureTest {

  private static final String MODULES_PACKAGE = "ao.hospitalao.modules.";

  @ArchTest
  static final ArchRule modulesOnlyDependOnOtherModulesThroughApplication =
      FreezingArchRule.freeze(
          classes()
              .that()
              .resideInAPackage("ao.hospitalao.modules..")
              .should(
                  new ArchCondition<>(
                      "depend on other modules only through their application package") {
                    @Override
                    public void check(JavaClass source, ConditionEvents events) {
                      String sourceModule = moduleName(source);
                      Set<String> violations = new HashSet<>();

                      for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        String targetModule = moduleName(target);

                        if (targetModule != null
                            && !targetModule.equals(sourceModule)
                            && !isApplicationClass(target, targetModule)) {
                          violations.add(source.getName() + " -> " + target.getName());
                        }
                      }

                      for (String violation : violations) {
                        events.add(SimpleConditionEvent.violated(source, violation));
                      }
                    }
                  }));

  private static String moduleName(JavaClass javaClass) {
    String packageName = javaClass.getPackageName();
    if (!packageName.startsWith(MODULES_PACKAGE)) {
      return null;
    }

    String moduleAndLayer = packageName.substring(MODULES_PACKAGE.length());
    int separator = moduleAndLayer.indexOf('.');
    return separator < 0 ? moduleAndLayer : moduleAndLayer.substring(0, separator);
  }

  private static boolean isApplicationClass(JavaClass javaClass, String moduleName) {
    String applicationPackage = MODULES_PACKAGE + moduleName + ".application";
    return javaClass.getPackageName().equals(applicationPackage)
        || javaClass.getPackageName().startsWith(applicationPackage + ".");
  }
}
