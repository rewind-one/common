package one.rewind.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2018/8/13 0013.
 */
public class ReflectModelUtil {

    /**
     *
     * @param map
     * @param beanClass
     * @return
     * @throws Exception
     */
    public static Object toObj(Map<String, Object> map, Class<?> beanClass) throws Exception {

        if (map == null || map.size()<=0)
            return null;

        Object obj = beanClass.newInstance();
        //获取关联的所有类，本类以及所有父类
        boolean ret = true;
        Class oo = obj.getClass();
        List<Class> clazzs = new ArrayList<>();
        while(ret){
            clazzs.add(oo);
            oo = oo.getSuperclass();
            if(oo == null || oo == Object.class)break;
        }

        for(int i=0;i<clazzs.size();i++){
            Field[] fields = clazzs.get(i).getDeclaredFields();
            for (Field field : fields) {
                int mod = field.getModifiers();
                if(Modifier.isStatic(mod) || Modifier.isFinal(mod)){
                    continue;
                }
                //由字符串转换回对象对应的类型
                if (field != null && map.get(field.getName()) != null) {

                    field.setAccessible(true);
                    field.set(obj, map.get(field.getName()));
                }
            }
        }
        return obj;
    }

    /**
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public static Map<String, Object> toMap(Object obj) throws Exception {

        if(obj == null){
            return null;
        }
        //获取关联的所有类，本类以及所有父类
        boolean ret = true;
        Class oo = obj.getClass();
        List<Class> clazzs = new ArrayList<Class>();
        while(ret){
            clazzs.add(oo);
            oo = oo.getSuperclass();
            if(oo == null || oo == Object.class)break;
        }

        Map<String, Object> map = new HashMap<String, Object>();

        for(int i=0;i<clazzs.size();i++){
            Field[] declaredFields = clazzs.get(i).getDeclaredFields();
            for (Field field : declaredFields) {
                int mod = field.getModifiers();
                //过滤 static 和 final 类型
                if(Modifier.isStatic(mod) || Modifier.isFinal(mod)){
                    continue;
                }
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            }
        }

        return map;
    }
}