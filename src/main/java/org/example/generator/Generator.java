package org.example.generator;

import org.example.annotations.Generatable;

import java.io.File;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Generator {
    private final Random random;
    private final int maxRecursionDepth;
    private final Map<Class<?>, ObjectGenerator<?>> primitiveGenerators;
    private final Map<Class<?>, List<Class<?>>> interfaceImplementations;
    private static final String packageName = "org.example.classes";

    private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
            String.class, Integer.class, int.class, Long.class, long.class,
            Double.class, double.class, Float.class, float.class,
            Boolean.class, boolean.class, Character.class, char.class,
            Byte.class, byte.class, Short.class, short.class
    );

    public Generator() {
        this(new Random(), 3);
    }

    public Generator(Random random, int maxRecursionDepth) {
        this.random = random;
        this.maxRecursionDepth = maxRecursionDepth;
        this.primitiveGenerators = createPrimitiveGenerators();
        this.interfaceImplementations = new ConcurrentHashMap<>();
    }

    public Object generateValueOfType(Class<?> clazz) throws RuntimeException {
        return generateValueOfType(clazz, 0);
    }

    private Object generateValueOfType(Class<?> clazz, int depth) throws RuntimeException {
        if (depth > maxRecursionDepth) {
            return getDefaultValue(clazz);
        }

        if (!isPrimitiveOrJavaType(clazz) && !clazz.isAnnotationPresent(Generatable.class)) {
            throw new IllegalArgumentException(
                new StringBuilder()
                    .append("Класс ")
                    .append(clazz.getCanonicalName())
                    .append(" не является Generatable")
                    .toString()
            );
        }

        try {
            if (primitiveGenerators.containsKey(clazz)) {
                return primitiveGenerators.get(clazz).generate();
            }

            if (clazz.isArray()) {
                return generateArray(clazz, depth);
            }

            if (Collection.class.isAssignableFrom(clazz)) {
                return generateCollection(clazz);
            }

            if (Map.class.isAssignableFrom(clazz)) {
                return generateMap();
            }

            if (clazz.isEnum()) {
                return generateEnum(clazz);
            }

            if (clazz.isInterface()) {
                Class<?> implementation = findImplementation(clazz);
                if (implementation != null) {
                    return generateValueOfType(implementation, depth);
                } else {
                    throw new IllegalArgumentException(
                        new StringBuilder()
                            .append("Интерфейс ")
                            .append(clazz.getCanonicalName())
                            .append(" не имеет Generatable классов")
                            .toString()
                    );
                }
            }

            return generateCustomClassInstance(clazz, depth);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private Object generateCustomClassInstance(Class<?> clazz, int depth) throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalArgumentException {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> chosenConstructor = constructors[random.nextInt(constructors.length)];

        chosenConstructor.setAccessible(true);

        Class<?>[] parameterTypes = chosenConstructor.getParameterTypes();
        Object[] generatedParameters = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            generatedParameters[i] = generateValueOfType(parameterTypes[i], depth + 1);
        }

        Object instance = chosenConstructor.newInstance(generatedParameters);

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (
                Modifier.isFinal(field.getModifiers()) ||
                Modifier.isStatic(field.getModifiers())
            ) {
                continue;
            }

            field.setAccessible(true);

            Object fieldValue = createFieldValue(field, depth);
            field.set(instance, fieldValue);
        }

        return instance;
    }

    private Object createFieldValue(Field field, int depth) throws RuntimeException {
        Class<?> fieldType = field.getType();

        if (Collection.class.isAssignableFrom(fieldType)) {
            return generateCollectionWithGenerics(field, depth);
        }

        if (Map.class.isAssignableFrom(fieldType)) {
            return generateMapWithGenerics(field, depth);
        }

        return generateValueOfType(fieldType, depth + 1);
    }

    private Collection<Object> generateCollection(Class<?> collectionType) {
        if (Set.class.isAssignableFrom(collectionType)) {
            return new HashSet<>();
        } else if (Queue.class.isAssignableFrom(collectionType)) {
            return new LinkedList<>();
        } else {
            return new ArrayList<>();
        }
    }

    private Object generateCollectionWithGenerics(Field field, int depth) throws RuntimeException {
        Type genericType = field.getGenericType();
        Collection<Object> collection = generateCollection(field.getType());

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();

            if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?> elementType) {
                int length = random.nextInt(1, 3);

                for (int i = 0; i < length; i++) {
                    Object element = generateValueOfType(elementType, depth + 1);
                    collection.add(element);
                }
            }
        }

        return collection;
    }

    private Map<Object, Object> generateMap() {
        return new HashMap<>();
    }

    private Object generateMapWithGenerics(Field field, int depth) throws RuntimeException {
        Type genericType = field.getGenericType();
        Map<Object, Object> map = generateMap();

        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();

            if (
                typeArgs.length == 2 &&
                typeArgs[0] instanceof Class<?> keyType &&
                typeArgs[1] instanceof Class<?> valueType
            ) {

                if (!IMMUTABLE_TYPES.contains(keyType) && !keyType.isEnum()) {
                    throw new IllegalArgumentException("Тип ключа для map должен быть immutable или enum: " + keyType);
                }

                int length = random.nextInt(1, 3);

                for (int i = 0; i < length; i++) {
                    Object key = generateValueOfType(keyType, depth + 1);
                    Object value = generateValueOfType(valueType, depth + 1);
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    private Object generateArray(Class<?> arrayType, int depth) {
        Class<?> componentType = arrayType.getComponentType();
        int length = random.nextInt(1, 3);
        Object array = Array.newInstance(componentType, length);

        for (int i = 0; i < length; i++) {
            Object element = generateValueOfType(componentType, depth + 1);
            Array.set(array, i, element);
        }

        return array;
    }

    private Object generateEnum(Class<?> enumType) {
        Object[] constants = enumType.getEnumConstants();

        if (constants.length == 0) {
            return null;
        } else {
            return constants[random.nextInt(constants.length)];
        }
    }

    private Class<?> findImplementation(Class<?> interfaceType) {
        List<Class<?>> listOfImplementations = interfaceImplementations.computeIfAbsent(interfaceType, this::scanImplementations);

        if (listOfImplementations.isEmpty()) {
            return null;
        } else {
            return listOfImplementations.get(random.nextInt(listOfImplementations.size()));
        }
    }

    private List<Class<?>> scanImplementations(Class<?> interfaceType) {
        List<Class<?>> implementations = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);

            if (resource != null && resource.getProtocol().equals("file")) {
                findClassesInDirectory(new File(resource.toURI()), packageName, interfaceType, implementations);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scanning implementations of " + interfaceType.getCanonicalName(), e);
        }

        return implementations;
    }

    private void findClassesInDirectory(
            File directory,
            String packageName,
            Class<?> interfaceType,
            List<Class<?>> implementations
    ) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, packageName + "." + file.getName(), interfaceType, implementations);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().replace(".class", "");

                try {
                    Class<?> clazz = Class.forName(className);

                    if (interfaceType.isAssignableFrom(clazz) && !clazz.isInterface() && clazz.isAnnotationPresent(Generatable.class)) {
                        implementations.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Can't load class " + className);
                }
            }
        }
    }

    private Object getDefaultValue(Class<?> clazz) {
        if (primitiveGenerators.containsKey(clazz)) {
            return primitiveGenerators.get(clazz).generate();
        }

        return null;
    }

    private boolean isPrimitiveOrJavaType(Class<?> clazz) {
        return primitiveGenerators.containsKey(clazz) ||
                clazz.isArray() ||
                Collection.class.isAssignableFrom(clazz) ||
                Map.class.isAssignableFrom(clazz) ||
                clazz.isEnum();
    }

    private Map<Class<?>, ObjectGenerator<?>> createPrimitiveGenerators() {
        Map<Class<?>, ObjectGenerator<?>> generators = new HashMap<>();

        generators.put(boolean.class, random::nextBoolean);
        generators.put(Boolean.class, random::nextBoolean);
        generators.put(int.class, () -> random.nextInt(100));
        generators.put(Integer.class, () -> random.nextInt(100));
        generators.put(long.class, () -> random.nextLong(100));
        generators.put(Long.class, () -> random.nextLong(100));
        generators.put(double.class, () -> random.nextDouble() * 100);
        generators.put(Double.class, () -> random.nextDouble() * 100);
        generators.put(float.class, () -> random.nextFloat() * 100);
        generators.put(Float.class, () -> random.nextFloat() * 100);
        generators.put(char.class, () -> (char)(random.nextInt('z' - 'a' + 1) + 'a'));
        generators.put(Character.class, () -> (char)(random.nextInt('z' - 'a' + 1) + 'a'));
        generators.put(byte.class, () -> (byte) random.nextInt(Byte.MAX_VALUE));
        generators.put(Byte.class, () -> (byte) random.nextInt(Byte.MAX_VALUE));
        generators.put(short.class, () -> (short) random.nextInt(Short.MAX_VALUE));
        generators.put(Short.class, () -> (short) random.nextInt(Short.MAX_VALUE));
        generators.put(String.class, () -> random.ints('a', 'z' + 1)
                .limit(random.nextInt(3,20))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString());

        return generators;
    }

    @FunctionalInterface
    private interface ObjectGenerator<T> {
        T generate();
    }
}