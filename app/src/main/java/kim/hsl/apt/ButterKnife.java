package kim.hsl.apt;

public class ButterKnife {

    /**
     * 在 Activity 中调用该方法, 绑定接口
     * @param target
     */
    public static void bind(Object target){
        String className = target.getClass().getName() + "_ViewBinder";

        try {
            // 通过反射得到 MainActivity_ViewBinder 类对象
            Class<?> clazz = Class.forName(className);

            if (IButterKnife.class.isAssignableFrom(clazz)){
                IButterKnife iButterKnife = (IButterKnife) clazz.newInstance();
                iButterKnife.bind(target);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

    }
}
