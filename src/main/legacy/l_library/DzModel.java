@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DzModel {

	String value() default "";
	String name();
	String desc();
	String tableName() default "";
}