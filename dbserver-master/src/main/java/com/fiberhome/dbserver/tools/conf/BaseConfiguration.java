package com.fiberhome.dbserver.tools.conf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.fiberhome.dbserver.tools.util.StringUtils;
import com.google.common.base.Preconditions;

/**
 * Provides access to configuration parameters.
 *
 * <h4 id="Resources">Resources</h4> Configurations are specified by resources.
 * A resource contains a set of name/value pairs as XML data. Each resource is
 * named by either a <code>String</code> or by a {@link Path}. If named by a
 * <code>String</code>, then the file path is examined for a file with that
 * name. If named by a <code>Path</code>, then the local filesystem is examined
 * directly.
 * <p>
 * Unless explicitly turned off, AsteriaDB by default specifies three resources,
 * loaded in-order from the 'conf'-related path:
 * <ol>
 * <li><a href="{root}/../conf/client-site.xml" />
 * <a href="{root}/../conf/server-site.xml" />
 * <a href="{root}/../conf/master-site.xml" /></li>
 * </ol>
 * Applications may add additional resources, which are loaded subsequent to
 * these resources in the order they are added.
 * </p>
 * <h4 id="FinalParams">Final Parameters</h4> Configuration parameters may be
 * declared <i>final</i>. Once a resource declares a value final, no
 * subsequently-loaded resource can alter that value. For example, one might
 * define a final parameter with: <tt><pre>
 *  &lt;property&gt;
 *    &lt;name&gt;dfs.hosts.include&lt;/name&gt;
 *    &lt;value&gt;${ASTERIADB_HOME}/conf/hosts.include&lt;/value&gt;
 *    <b>&lt;final&gt;true&lt;/final&gt;</b>
 *  &lt;/property&gt;</pre></tt> Administrators typically define parameters as
 * final in <tt>server-site.xml</tt> for values that user applications may not
 * alter.
 * <h4 id="VariableExpansion">Variable Expansion</h4>
 * <p>
 * Value strings are first processed for <i>variable expansion</i>. The
 * available properties are:
 * <ol>
 * <li>Other properties defined in this Configuration; and, if a name is
 * undefined here,</li>
 * <li>Properties in {@link System#getProperties()}.</li>
 * </ol>
 * </p>
 * For example, if a configuration resource contains the following property
 * definitions: <tt><pre>
 *  &lt;property&gt;
 *    &lt;name&gt;basedir&lt;/name&gt;
 *    &lt;value&gt;/user/${<i>user.name</i>}&lt;/value&gt;
 *  &lt;/property&gt;
 *  
 *  &lt;property&gt;
 *    &lt;name&gt;tempdir&lt;/name&gt;
 *    &lt;value&gt;${<i>basedir</i>}/tmp&lt;/value&gt;
 *  &lt;/property&gt;</pre></tt> When <tt>conf.get("tempdir")</tt> is called,
 * then <tt>${<i>basedir</i>}</tt> will be resolved to another property in this
 * Configuration, while <tt>${<i>user.name</i>}</tt> would then ordinarily be
 * resolved to the value of the System property with that name. By default,
 * warnings will be given to any deprecated configuration parameters and these
 * are suppressible by configuring
 */
public class BaseConfiguration implements Iterable<Entry<String, String>>
{

    private static final Logger LOG = LoggerFactory.getLogger(BaseConfiguration.class);

    private final Object lockObj = new Object();

    private static final Logger LOG_DEPRECATION = LoggerFactory
        .getLogger("com.fiberhome.asteriadb.conf.Configuration.deprecation");

    /** Default table name in asteriadb which is rejected to configurate */
    public static final String DEFAULT_ASTERIADB_TABLENAME = "default";

    /** 是否采用安静模式，配置中不输出LOG日志 */
    private boolean quietmode = true;

    /**
     * <p>
     * 资源 <br>
     * 当前支持的资源类型有： 流, URL, 配置名, 文件
     * </p>
     */
    private static class Resource
    {
        private final Object resource;
        private final String name;

        public Resource(Object resource)
        {
            this(resource, resource.toString());
        }

        public Resource(Object resource, String name)
        {
            this.resource = resource;
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public Object getResource()
        {
            return resource;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    /**
     * List of configuration resources.
     */
    private ArrayList<Resource> resources = new ArrayList<Resource>();

    /**
     * The value reported as the setting resource when a key is set by code rather
     * than a file resource by dumpConfiguration.
     */
    static final String UNKNOWN_RESOURCE = "Unknown";

    /**
     * List of configuration parameters marked <b>final</b>.
     */
    private Set<String> finalParameters = new HashSet<String>();

    /**
     * 是否装载默认资源
     */
    private boolean loadDefaults = true;

    /**
     * Configuration objects
     */
    private static final WeakHashMap<BaseConfiguration, Object> REGISTRY = new WeakHashMap<BaseConfiguration, Object>();

    /**
     * List of default Resources. Resources are loaded in the order of the list
     * entries
     */
    private static final CopyOnWriteArrayList<String> DEFAULTRESOURCES = new CopyOnWriteArrayList<String>();

    private static final Map<ClassLoader, Map<String, WeakReference<Class<?>>>> CACHE_CLASSES = 
        new WeakHashMap<ClassLoader, Map<String, WeakReference<Class<?>>>>();

    /**
     * Sentinel value to store negative cache results in {@link #CACHE_CLASSES}.
     */
    private static final Class<?> NEGATIVE_CACHE_SENTINEL = NegativeCacheSentinel.class;

    /**
     * Stores the mapping of key to the resource which modifies or loads the key
     * most recently
     */
    private HashMap<String, String[]> updatingResource;

    /**
     * Class to keep the information about the keys which replace the deprecated
     * ones. This class stores the new keys which replace the deprecated keys and
     * also gives a provision to have a custom message for each of the deprecated
     * key that is being replaced. It also provides method to get the appropriate
     * warning message which can be logged whenever the deprecated key is used.
     */
    private static class DeprecatedKeyInfo
    {
        private final String[] newKeys;
        private final String customMessage;
        private final AtomicBoolean accessed = new AtomicBoolean(false);

        DeprecatedKeyInfo(String[] newKeys, String customMessage)
        {
            this.newKeys = newKeys;
            this.customMessage = customMessage;
        }

        /**
         * Method to provide the warning message. It gives the custom message if
         * non-null, and default message otherwise.
         *
         * @param key the associated deprecated key.
         * @return message that is to be logged when a deprecated key is used.
         */
        private final String getWarningMessage(String key)
        {
            String warningMessage;
            if (customMessage == null)
            {
                StringBuilder message = new StringBuilder(key);
                String deprecatedKeySuffix = " is deprecated. Instead, use ";
                message.append(deprecatedKeySuffix);

                for (int i = 0; i < newKeys.length; i++)
                {
                    message.append(newKeys[i]);
                    if (i != newKeys.length - 1)
                    {
                        message.append(", ");
                    }
                }
                warningMessage = message.toString();
            }
            else
            {
                warningMessage = customMessage;
            }
            return warningMessage;
        }

        boolean getAndSetAccessed()
        {
            return accessed.getAndSet(true);
        }

        public void clearAccessed()
        {
            accessed.set(false);
        }
    }

    /**
     * A pending addition to the global set of deprecated keys.
     */
    public static class DeprecationDelta
    {
        private final String key;
        private final String[] newKeys;
        private final String customMessage;

        DeprecationDelta(String key, String[] newKeys, String customMessage)
        {
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(newKeys);
            Preconditions.checkArgument(newKeys.length > 0);
            this.key = key;
            this.newKeys = newKeys;
            this.customMessage = customMessage;
        }

        public DeprecationDelta(String key, String newKey, String customMessage)
        {
            this(key, new String[] { newKey }, customMessage);
        }

        public DeprecationDelta(String key, String newKey)
        {
            this(key, new String[] { newKey }, null);
        }

        public String getKey()
        {
            return key;
        }

        public String[] getNewKeys()
        {
            return newKeys;
        }

        public String getCustomMessage()
        {
            return customMessage;
        }
    }

    /**
     * The set of all keys which are deprecated. DeprecationContext objects are
     * immutable.
     */
    private static class DeprecationContext
    {
        /**
         * Stores the deprecated keys, the new keys which replace the deprecated keys
         * and custom message(if any provided).
         */
        private final Map<String, DeprecatedKeyInfo> deprecatedKeyMap;

        /**
         * Stores a mapping from superseding keys to the keys which they deprecate.
         */
        private final Map<String, String> reverseDeprecatedKeyMap;

        /**
         * Create a new DeprecationContext by copying a previous DeprecationContext and
         * adding some deltas.
         *
         * @param other The previous deprecation context to copy, or null to start from
         *            nothing.
         * @param deltas The deltas to apply.
         */
        DeprecationContext(DeprecationContext other, DeprecationDelta[] deltas)
        {
            HashMap<String, DeprecatedKeyInfo> newDeprecatedKeyMap = new HashMap<String, DeprecatedKeyInfo>();
            HashMap<String, String> newReverseDeprecatedKeyMap = new HashMap<String, String>();

            if (other != null)
            {
                for (Entry<String, DeprecatedKeyInfo> entry : other.deprecatedKeyMap.entrySet())
                {
                    newDeprecatedKeyMap.put(entry.getKey(), entry.getValue());
                }

                for (Entry<String, String> entry : other.reverseDeprecatedKeyMap.entrySet())
                {
                    newReverseDeprecatedKeyMap.put(entry.getKey(), entry.getValue());
                }
            }

            for (DeprecationDelta delta : deltas)
            {
                if (!newDeprecatedKeyMap.containsKey(delta.getKey()))
                {
                    DeprecatedKeyInfo newKeyInfo = new DeprecatedKeyInfo(delta.getNewKeys(), delta.getCustomMessage());
                    newDeprecatedKeyMap.put(delta.key, newKeyInfo);

                    for (String newKey : delta.getNewKeys())
                    {
                        newReverseDeprecatedKeyMap.put(newKey, delta.key);
                    }
                }
            }

            this.deprecatedKeyMap = newDeprecatedKeyMap;
            this.reverseDeprecatedKeyMap = newReverseDeprecatedKeyMap;
        }

        Map<String, DeprecatedKeyInfo> getDeprecatedKeyMap()
        {
            return deprecatedKeyMap;
        }

        Map<String, String> getReverseDeprecatedKeyMap()
        {
            return reverseDeprecatedKeyMap;
        }
    }

    private static DeprecationDelta[] defaultDeprecations = new DeprecationDelta[] {
        new DeprecationDelta("asteriadb.default.tablename", DEFAULT_ASTERIADB_TABLENAME) };

    /**
     * The global DeprecationContext.
     */
    private static AtomicReference<DeprecationContext> deprecationContext = new AtomicReference<DeprecationContext>(
        new DeprecationContext(null, defaultDeprecations));

    /**
     * Adds a set of deprecated keys to the global deprecations. This method is
     * lockless. It works by means of creating a new DeprecationContext based on the
     * old one, and then atomically swapping in the new context. If someone else
     * updated the context in between us reading the old context and swapping in the
     * new one, we try again until we win the race.
     *
     * @param deltas The deprecations to add.
     */
    public static void addDeprecations(DeprecationDelta[] deltas)
    {
        DeprecationContext prev;
        DeprecationContext next;
        do
        {
            prev = deprecationContext.get();
            next = new DeprecationContext(prev, deltas);
        } while (!deprecationContext.compareAndSet(prev, next));
    }

    /**
     * checks whether the given <code>key</code> is deprecated.
     *
     * @param key the parameter which is to be checked for deprecation
     * @return <code>true</code> if the key is deprecated and <code>false</code>
     *         otherwise.
     */
    public static boolean isDeprecated(String key)
    {
        return deprecationContext.get().getDeprecatedKeyMap().containsKey(key);
    }

    /**
     * Sets all deprecated properties that are not currently set but have a
     * corresponding new property that is set. Useful for iterating the properties
     * when all deprecated properties for currently set properties need to be
     * present.
     */
    public void setDeprecatedProperties()
    {
        DeprecationContext deprecations = deprecationContext.get();
        Properties props = getProps();
        Properties overlay = getOverlay();
        for (Entry<String, DeprecatedKeyInfo> entry : deprecations.getDeprecatedKeyMap().entrySet())
        {
            String depKey = entry.getKey();
            if (!overlay.contains(depKey))
            {
                for (String newKey : entry.getValue().newKeys)
                {
                    String val = overlay.getProperty(newKey);
                    if (val != null)
                    {
                        props.setProperty(depKey, val);
                        overlay.setProperty(depKey, val);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Checks for the presence of the property <code>name</code> in the deprecation
     * map. Returns the first of the list of new keys if present in the deprecation
     * map or the <code>name</code> itself. If the property is not presently set but
     * the property map contains an entry for the deprecated key, the value of the
     * deprecated key is set as the value for the provided property name.
     *
     * @param name the property name
     * @return the first property in the list of properties mapping the
     *         <code>name</code> or the <code>name</code> itself.
     */
    private String[] handleDeprecation(DeprecationContext deprecations, String name)
    {
        if (null != name)
        {
            name = name.trim();
        }

        ArrayList<String> names = new ArrayList<String>();
        if (isDeprecated(name))
        {
            DeprecatedKeyInfo keyInfo = deprecations.getDeprecatedKeyMap().get(name);
            warnOnceIfDeprecated(deprecations, name);
            for (String newKey : keyInfo.newKeys)
            {
                if (newKey != null)
                {
                    names.add(newKey);
                }
            }
        }

        if (names.size() == 0)
        {
            names.add(name);
        }

        for (String n : names)
        {
            String deprecatedKey = deprecations.getReverseDeprecatedKeyMap().get(n);
            if (deprecatedKey != null && !getOverlay().containsKey(n) && getOverlay().containsKey(deprecatedKey))
            {
                getProps().setProperty(n, getOverlay().getProperty(deprecatedKey));
                getOverlay().setProperty(n, getOverlay().getProperty(deprecatedKey));
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private void handleDeprecation()
    {
        LOG.debug("Handling deprecation for all properties in config...");
        DeprecationContext deprecations = deprecationContext.get();
        Set<Object> keys = new HashSet<Object>();
        keys.addAll(getProps().keySet());
        for (Object item : keys)
        {
            LOG.debug("Handling deprecation for " + (String) item);
            handleDeprecation(deprecations, (String) item);
        }
    }

    static
    {
        addDefaultResource("conf/zk-site.xml");
        // Default resource not be provided, thus vainly thinking that do
        // nothing but get configuration.
    }

    private Properties properties;
    private Properties overlay;
    private ClassLoader classLoader;

    {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null)
        {
            classLoader = BaseConfiguration.class.getClassLoader();
        }
    }

    // public static BaseConfiguration getDefaultConfiguration()
    // {
    // return Holder.conf;
    // }
    //
    // static class Holder
    // {
    // static BaseConfiguration conf = new BaseConfiguration();
    // }

    /**
     * <p>
     * 构造器
     * </p>
     * @当前不支持拒绝装载默认配置的操作
     */
    public BaseConfiguration()
    {
        this(true);
    }

    /**
     * A new configuration where the behavior of reading from the default resources
     * can be turned off. If the parameter {@code loadDefaults} is false, the new
     * instance will not load resources from the default files.
     *
     * @param loadDefaults specifies whether to load from the default files
     */
    public BaseConfiguration(boolean loadDefaults)
    {
        this.loadDefaults = loadDefaults;
        updatingResource = new HashMap<String, String[]>();
        synchronized (BaseConfiguration.class)
        {
            REGISTRY.put(this, null);
        }
        // 初始化加载出所有配置
        getProps();
    }

    /**
     * A new configuration with the same settings cloned from another.
     *
     * @param other the configuration from which to clone settings.
     */
    @SuppressWarnings("unchecked")
    public BaseConfiguration(BaseConfiguration other)
    {
        this.resources = (ArrayList<Resource>) other.resources.clone();
        synchronized (lockObj)
        {
            if (other.properties != null)
            {
                this.properties = (Properties) other.properties.clone();
            }

            if (other.overlay != null)
            {
                this.overlay = (Properties) other.overlay.clone();
            }

            this.updatingResource = new HashMap<String, String[]>(other.updatingResource);
            this.finalParameters = new HashSet<String>(other.finalParameters);
        }

        synchronized (BaseConfiguration.class)
        {
            REGISTRY.put(this, null);
        }
        this.classLoader = other.classLoader;
        this.loadDefaults = other.loadDefaults;
        setQuietMode(other.getQuietMode());
    }

    /**
     * Add a default resource. Resources are loaded in the order of the resources
     * added.
     *
     * @param name file name. File should be present in the classpath.
     */
    private static synchronized void addDefaultResource(String name)
    {
        if (!DEFAULTRESOURCES.contains(name))
        {
            DEFAULTRESOURCES.add(name);
            for (BaseConfiguration conf : REGISTRY.keySet())
            {
                if (conf.loadDefaults)
                {
                    conf.reloadConfiguration();
                }
            }
        }
    }

    /**
     * Add a configuration resource. The properties of this resource will override
     * properties of previously added resources, unless they were marked
     * <a href="#Final">final</a>.
     *
     * @param name resource to be added, the classpath is examined for a file with
     *            that name.
     */
    public void addResource(String name)
    {
        addResource(getPathObject(name));
    }

    /**
     * Add a configuration resource. The properties of this resource will override
     * properties of previously added resources, unless they were marked
     * <a href="#Final">final</a>.
     *
     * @param url url of the resource to be added, the local filesystem is examined
     *            directly to find the resource, without referring to the classpath.
     */
    public void addResource(URL url)
    {
        addResourceObject(new Resource(url));
    }

    /**
     * Add a configuration resource. The properties of this resource will override
     * properties of previously added resources, unless they were marked
     * <a href="#Final">final</a>.
     *
     * @param file file-path of resource to be added, the local filesystem is
     *            examined directly to find the resource, without referring to the
     *            classpath.
     */
    public void addResource(Path file)
    {
        addResourceObject(new Resource(file));
    }

    /**
     * Add a configuration resource. The properties of this resource will override
     * properties of previously added resources, unless they were marked
     * <a href="#Final">final</a>. WARNING: The contents of the InputStream will be
     * cached, by this method. So use this sparingly because it does increase the
     * memory consumption.
     *
     * @param in InputStream to deserialize the object from. In will be read from
     *            when a get or set is called next. After it is read the stream will
     *            be closed.
     */
    public void addResource(InputStream in)
    {
        addResourceObject(new Resource(in));
    }

    /**
     * Add a configuration resource. The properties of this resource will override
     * properties of previously added resources, unless they were marked
     * <a href="#Final">final</a>.
     * 
     * @param in InputStream to deserialize the object from.
     * @param name the name of the resource because InputStream.toString is not very
     *            descriptive some times.
     */
    public void addResource(InputStream in, String name)
    {
        addResourceObject(new Resource(in, name));
    }

    /**
     * Add a configuration resource. The properties of this resource will override
     * properties of previously added resources, unless they were marked
     * <a href="#Final">final</a>.
     *
     * @param conf Configuration object from which to load properties
     */
    public void addResource(BaseConfiguration conf)
    {
        addResourceObject(new Resource(conf.getProps()));
    }

    /**
     * Reload configuration from previously added resources. This method will clear
     * all the configuration read from the added resources, and final parameters.
     * This will make the resources to be read again before accessing the values.
     * Values that are added via set methods will overlay values read from the
     * resources.
     */
    public synchronized void reloadConfiguration()
    {
        properties = null; // trigger reload
        finalParameters.clear(); // clear site-limits
    }

    private synchronized void addResourceObject(Resource resource)
    {
        resources.add(resource); // add to resources
        reloadConfiguration();
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");

    /** 最大20次扩展 */
    private static final int MAX_SUBST = 20;

    private String substituteVars(String expr)
    {
        if (expr == null)
        {
            return null;
        }

        Matcher match = VAR_PATTERN.matcher("");
        String eval = expr;
        for (int s = 0; s < MAX_SUBST; s++)
        {
            match.reset(eval);
            if (!match.find())
            {
                return eval;
            }

            String var = match.group();
            var = var.substring(2, var.length() - 1); // remove ${ .. }
            String val = null;
            try
            {
                val = System.getProperty(var);
            }
            catch (SecurityException e)
            {
                LOG.warn("Unexpected SecurityException in Configuration", e);
            }

            if (val == null)
            {
                val = getRaw(var);
            }
            if (val == null)
            {
                return eval; // return literal ${var}: var is unbound
            }
            // substitute
            eval = eval.substring(0, match.start()) + val + eval.substring(match.end());
        }
        throw new IllegalStateException("Variable substitution depth too large: " + MAX_SUBST + " " + expr);
    }

    /**
     * Get the value of the <code>name</code> property, <code>null</code> if no such
     * property exists. If the key is deprecated, it returns the value of the first
     * key which replaces the deprecated key and is not null. Values are processed
     * for <a href="#VariableExpansion">variable expansion</a> before being
     * returned.
     * 
     * @param name the property name, will be trimmed before get value.
     * @return the value of the <code>name</code> or its replacing property, or null
     *         if no such property exists.
     */
    public String get(String name)
    {
        String[] names = handleDeprecation(deprecationContext.get(), name);
        String result = null;
        for (String n : names)
        {
            result = substituteVars(getProps().getProperty(n));
        }
        return result;
    }

    /**
     * Get the value of the <code>name</code>. If the key is deprecated, it returns
     * the value of the first key which replaces the deprecated key and is not null.
     * If no such property exists, then <code>defaultValue</code> is returned.
     *
     * @param name property name, will be trimmed before get value.
     * @param defaultValue default value.
     * @return property value, or <code>defaultValue</code> if the property doesn't
     *         exist.
     */
    public String get(String name, String defaultValue)
    {
        String[] names = handleDeprecation(deprecationContext.get(), name);
        String result = null;
        for (String n : names)
        {
            result = substituteVars(getProps().getProperty(n, defaultValue));
        }
        return result;
    }

    /**
     * Get the value of the <code>name</code> property as a trimmed
     * <code>String</code>, <code>null</code> if no such property exists. If the key
     * is deprecated, it returns the value of the first key which replaces the
     * deprecated key and is not null Values are processed for
     * <a href="#VariableExpansion">variable expansion</a> before being returned.
     *
     * @param name the property name.
     * @return the value of the <code>name</code> or its replacing property, or null
     *         if no such property exists.
     */
    public String getTrimmed(String name)
    {
        String value = get(name);

        if (null == value)
        {
            return null;
        }
        else
        {
            return value.trim();
        }
    }

    /**
     * Get the value of the <code>name</code> property as a trimmed
     * <code>String</code>, <code>defaultValue</code> if no such property exists.
     * See @{Configuration#getTrimmed} for more details.
     *
     * @param name the property name.
     * @param defaultValue the property default value.
     * @return the value of the <code>name</code> or defaultValue if it is not set.
     */
    public String getTrimmed(String name, String defaultValue)
    {
        String ret = getTrimmed(name);
        return ret == null ? defaultValue : ret;
    }

    /**
     * Get the value of the <code>name</code> property, without doing
     * <a href="#VariableExpansion">variable expansion</a>.If the key is deprecated,
     * it returns the value of the first key which replaces the deprecated key and
     * is not null.
     *
     * @param name the property name.
     * @return the value of the <code>name</code> property or its replacing property
     *         and null if no such property exists.
     */
    public String getRaw(String name)
    {
        String[] names = handleDeprecation(deprecationContext.get(), name);
        String result = null;
        for (String n : names)
        {
            result = getProps().getProperty(n);
        }
        return result;
    }

    /**
     * Returns alternative names (non-deprecated keys or previously-set deprecated
     * keys) for a given non-deprecated key. If the given key is deprecated, return
     * null.
     *
     * @param name property name.
     * @return alternative names.
     */
    private String[] getAlternativeNames(String name)
    {
        String[] altNames = null;
        DeprecatedKeyInfo keyInfo = null;
        DeprecationContext cur = deprecationContext.get();
        String depKey = cur.getReverseDeprecatedKeyMap().get(name);
        if (depKey != null)
        {
            keyInfo = cur.getDeprecatedKeyMap().get(depKey);
            if (keyInfo.newKeys.length > 0)
            {
                if (getProps().containsKey(depKey))
                {
                    // if deprecated key is previously set explicitly
                    List<String> list = new ArrayList<String>();
                    list.addAll(Arrays.asList(keyInfo.newKeys));
                    list.add(depKey);
                    altNames = list.toArray(new String[list.size()]);
                }
                else
                {
                    altNames = keyInfo.newKeys;
                }
            }
        }
        return altNames;
    }

    /**
     * Set the <code>value</code> of the <code>name</code> property. If
     * <code>name</code> is deprecated or there is a deprecated name associated to
     * it, it sets the value to both names. Name will be trimmed before put into
     * configuration.
     *
     * @param name property name.
     * @param value property value.
     */
    public void set(String name, String value)
    {
        set(name, value, null);
    }

    /**
     * Set the <code>value</code> of the <code>name</code> property. If
     * <code>name</code> is deprecated, it also sets the <code>value</code> to the
     * keys that replace the deprecated key. Name will be trimmed before put into
     * configuration.
     *
     * @param name property name.
     * @param value property value.
     * @param source the place that this configuration value came from (For
     *            debugging).
     * @throws IllegalArgumentException when the value or name is null.
     */
    private void set(String name, String value, String source)
    {
        Preconditions.checkArgument(name != null, "Property name must not be null");
        Preconditions.checkArgument(value != null, "The value of property " + name + " must not be null");
        name = name.trim();
        DeprecationContext deprecations = deprecationContext.get();
        if (deprecations.getDeprecatedKeyMap().isEmpty())
        {
            getProps();
        }
        getOverlay().setProperty(name, value);
        getProps().setProperty(name, value);
        String newSource = (source == null ? "programatically" : source);

        if (!isDeprecated(name))
        {
            updatingResource.put(name, new String[] { newSource });
            String[] altNames = getAlternativeNames(name);
            if (altNames != null)
            {
                for (String n : altNames)
                {
                    if (!n.equals(name))
                    {
                        getOverlay().setProperty(n, value);
                        getProps().setProperty(n, value);
                        updatingResource.put(n, new String[] { newSource });
                    }
                }
            }
        }
        else
        {
            String[] names = handleDeprecation(deprecationContext.get(), name);
            String altSource = "because " + name + " is deprecated";
            for (String n : names)
            {
                getOverlay().setProperty(n, value);
                getProps().setProperty(n, value);
                updatingResource.put(n, new String[] { altSource });
            }
        }
    }

    private void warnOnceIfDeprecated(DeprecationContext deprecations, String name)
    {
        DeprecatedKeyInfo keyInfo = deprecations.getDeprecatedKeyMap().get(name);
        if (keyInfo != null && !keyInfo.getAndSetAccessed())
        {
            LOG_DEPRECATION.info(keyInfo.getWarningMessage(name));
        }
    }

    private synchronized Properties getOverlay()
    {
        if (overlay == null)
        {
            overlay = new Properties();
        }
        return overlay;
    }

    /**
     * Get the value of the <code>name</code> property as an <code>int</code>. If no
     * such property exists, the provided default value is returned, or if the
     * specified value is not a valid <code>int</code>, then an error is thrown.
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return property value
     */
    public int getInt(String name, int defaultValue)
    {
        String valueString = getTrimmed(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        String hexString = getHexDigits(valueString);
        if (hexString != null)
        {
            return Integer.parseInt(hexString, 16);
        }
        return Integer.parseInt(valueString);
    }

    /**
     * Get the value of the <code>name</code> property as a set of comma-delimited
     * <code>int</code> values. If no such property exists, an empty array is
     * returned.
     *
     * @param name property name
     * @return property value interpreted as an array of comma-delimited
     *         <code>int</code> values
     */
    public int[] getInts(String name)
    {
        String[] strings = getTrimmedStrings(name);
        int[] ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++)
        {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    /**
     * Get the value of the <code>name</code> property as a <code>long</code>. If no
     * such property exists, the provided default value is returned, or if the
     * specified value is not a valid <code>long</code>, then an error is thrown.
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return property value
     */
    public long getLong(String name, long defaultValue)
    {
        String valueString = getTrimmed(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        String hexString = getHexDigits(valueString);
        if (hexString != null)
        {
            return Long.parseLong(hexString, 16);
        }
        return Long.parseLong(valueString);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>long</code> or
     * human readable format. If no such property exists, the provided default value
     * is returned, or if the specified value is not a valid <code>long</code> or
     * human readable format, then an error is thrown. You can use the following
     * suffix (case insensitive): k(kilo), m(mega), g(giga), t(tera), p(peta),
     * e(exa)
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return property value
     */
    public long getLongBytes(String name, long defaultValue)
    {
        String valueString = getTrimmed(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        return StringUtils.TraditionalBinaryPrefix.string2long(valueString);
    }

    private String getHexDigits(String value)
    {
        boolean negative = false;
        String str = value;
        String hexString = null;
        if (value.startsWith("-"))
        {
            negative = true;
            str = value.substring(1);
        }
        if (str.startsWith("0x") || str.startsWith("0X"))
        {
            hexString = str.substring(2);
            if (negative)
            {
                hexString = "-" + hexString;
            }
            return hexString;
        }
        return null;
    }

    /**
     * Get the value of the <code>name</code> property as a <code>float</code>. If
     * no such property exists, the provided default value is returned, or if the
     * specified value is not a valid <code>float</code>, then an error is thrown.
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return property value
     */
    public float getFloat(String name, float defaultValue)
    {
        String valueString = getTrimmed(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        return Float.parseFloat(valueString);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>double</code>. If
     * no such property exists, the provided default value is returned, or if the
     * specified value is not a valid <code>double</code>, then an error is thrown.
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return property value
     */
    public double getDouble(String name, double defaultValue)
    {
        String valueString = getTrimmed(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        return Double.parseDouble(valueString);
    }

    /**
     * Get the value of the <code>name</code> property as a <code>boolean</code> .
     * If no such property is specified, or if the specified value is not a valid
     * <code>boolean</code>, then <code>defaultValue</code> is returned.
     *
     * @param name property name.
     * @param defaultValue default value.
     * @return property value as a <code>boolean</code>, or
     *         <code>defaultValue</code>.
     */
    public boolean getBoolean(String name, boolean defaultValue)
    {
        String valueString = getTrimmed(name);
        if (null == valueString || valueString.isEmpty())
        {
            return defaultValue;
        }

        valueString = valueString.toLowerCase();

        if ("true".equals(valueString))
        {
            return true;
        }
        else if ("false".equals(valueString))
        {
            return false;
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Return value matching this enumerated type.
     *
     * @param <T> 泛型
     * @param name Property name
     * @param defaultValue Value returned if no mapping exists
     * @return 枚举泛型
     */
    public <T extends Enum<T>> T getEnum(String name, T defaultValue)
    {
        final String val = get(name);
        return null == val ? defaultValue : Enum.valueOf(defaultValue.getDeclaringClass(), val);
    }

    enum ParsedTimeDuration
    {
        NS
        {
            TimeUnit unit()
            {
                return TimeUnit.NANOSECONDS;
            }

            String suffix()
            {
                return "ns";
            }
        },
        US
        {
            TimeUnit unit()
            {
                return TimeUnit.MICROSECONDS;
            }

            String suffix()
            {
                return "us";
            }
        },
        MS
        {
            TimeUnit unit()
            {
                return TimeUnit.MILLISECONDS;
            }

            String suffix()
            {
                return "ms";
            }
        },
        S
        {
            TimeUnit unit()
            {
                return TimeUnit.SECONDS;
            }

            String suffix()
            {
                return "s";
            }
        },
        M
        {
            TimeUnit unit()
            {
                return TimeUnit.MINUTES;
            }

            String suffix()
            {
                return "m";
            }
        },
        H
        {
            TimeUnit unit()
            {
                return TimeUnit.HOURS;
            }

            String suffix()
            {
                return "h";
            }
        },
        D
        {
            TimeUnit unit()
            {
                return TimeUnit.DAYS;
            }

            String suffix()
            {
                return "d";
            }
        };
        abstract TimeUnit unit();

        abstract String suffix();

        static ParsedTimeDuration unitFor(String s)
        {
            for (ParsedTimeDuration ptd : values())
            {
                // iteration order is in decl order, so SECONDS matched last
                if (s.endsWith(ptd.suffix()))
                {
                    return ptd;
                }
            }
            return null;
        }

        static ParsedTimeDuration unitFor(TimeUnit unit)
        {
            for (ParsedTimeDuration ptd : values())
            {
                if (ptd.unit() == unit)
                {
                    return ptd;
                }
            }
            return null;
        }
    }

    /**
     * Return time duration in the given time unit. Valid units are encoded in
     * properties as suffixes: nanoseconds (ns), microseconds (us), milliseconds
     * (ms), seconds (s), minutes (m), hours (h), and days (d).
     *
     * @param name Property name
     * @param defaultValue Value returned if no mapping exists.
     * @param unit Unit to convert the stored property, if it exists.
     * @return long
     * @throws NumberFormatException If the property stripped of its unit is not a
     *             number
     */
    public Long getTimeDuration(String name, long defaultValue, TimeUnit unit)
    {
        String vStr = get(name);
        if (null == vStr)
        {
            return defaultValue;
        }
        vStr = vStr.trim();
        ParsedTimeDuration vUnit = ParsedTimeDuration.unitFor(vStr);
        if (null == vUnit)
        {
            LOG.warn("No unit for " + name + "(" + vStr + ") assuming " + unit);
            vUnit = ParsedTimeDuration.unitFor(unit);
        }
        else
        {
            vStr = vStr.substring(0, vStr.lastIndexOf(vUnit.suffix()));
        }

        if (vUnit != null)
        {
            return unit.convert(Long.parseLong(vStr), vUnit.unit());
        }
        else
        {
            LOG.error("Parse ParsedTimeDuration failed.");
            throw new IllegalArgumentException(
                "No corresponding ParsedTimeDuration parsed, check the parameter: " + name + ".");
        }

    }

    enum ParsedMemoryDuration
    {
        B
        {
            MemoryUnit unit()
            {
                return MemoryUnit.BYTES;
            }

            String suffix()
            {
                return "B";
            }
        },
        K
        {
            MemoryUnit unit()
            {
                return MemoryUnit.KILOBYTES;
            }

            String suffix()
            {
                return "K";
            }
        },
        M
        {
            MemoryUnit unit()
            {
                return MemoryUnit.MEGABYTES;
            }

            String suffix()
            {
                return "M";
            }
        },
        G
        {
            MemoryUnit unit()
            {
                return MemoryUnit.GIGABYTES;
            }

            String suffix()
            {
                return "G";
            }
        },
        T
        {
            MemoryUnit unit()
            {
                return MemoryUnit.TERABYTES;
            }

            String suffix()
            {
                return "T";
            }
        };

        abstract MemoryUnit unit();

        abstract String suffix();

        static ParsedMemoryDuration unitFor(String s)
        {
            for (ParsedMemoryDuration ptd : values())
            {
                // iteration order is in decl order, so BYTES matched last
                if (s.endsWith(ptd.suffix()))
                {
                    return ptd;
                }
            }
            return null;
        }

        static ParsedMemoryDuration unitFor(MemoryUnit unit)
        {
            for (ParsedMemoryDuration ptd : values())
            {
                if (ptd.unit() == unit)
                {
                    return ptd;
                }
            }
            return null;
        }
    }

    /**
     * Return time duration in the given memory unit. Valid units are encoded in
     * properties as suffixes: BITS (b), BYTES (B), KILOBYTES (KB), MEGABYTES (MB),
     * GIGABYTES (GB).
     *
     * @param name Property name
     * @param defaultValue Value returned if no mapping exists.
     * @param unit Unit to convert the stored property, if it exists.
     * @return long
     * @throws NumberFormatException If the property stripped of its unit is not a
     *             number
     */
    public long getMemoryDuration(String name, long defaultValue, MemoryUnit unit)
    {
        String vStr = get(name);
        if (null == vStr)
        {
            return defaultValue;
        }
        vStr = vStr.trim();
        ParsedMemoryDuration vUnit = ParsedMemoryDuration.unitFor(vStr);
        if (null == vUnit)
        {
            LOG.warn("No unit for " + name + "(" + vStr + ") assuming " + unit);
            vUnit = ParsedMemoryDuration.unitFor(unit);
        }
        else
        {
            vStr = vStr.substring(0, vStr.lastIndexOf(vUnit.suffix()));
        }
        return unit.convert(Long.parseLong(vStr), vUnit.unit());
    }

    /**
     * Get the value of the <code>name</code> property as a <code>Pattern</code> .
     * If no such property is specified, or if the specified value is not a valid
     * <code>Pattern</code>, then <code>DefaultValue</code> is returned.
     *
     * @param name property name
     * @param defaultValue default value
     * @return property value as a compiled Pattern, or defaultValue
     */
    public Pattern getPattern(String name, Pattern defaultValue)
    {
        String valString = get(name);
        if (null == valString || valString.isEmpty())
        {
            return defaultValue;
        }

        try
        {
            return Pattern.compile(valString);
        }
        catch (PatternSyntaxException exec)
        {
            LOG.warn("Regular expression '" + valString + "' for property '" + name + "' not valid. Using default",
                exec);
            return defaultValue;
        }
    }

    /**
     * Gets information about why a property was set. Typically this is the path to
     * the resource objects (file, URL, etc.) the property came from, but it can
     * also indicate that it was set programatically, or because of the command
     * line.
     *
     * @param name - The property name to get the source of.
     * @return null - If the property or its source wasn't found. Otherwise, returns
     *         a list of the sources of the resource. The older sources are the
     *         first ones in the list. So for example if a configuration is set from
     *         the command line, and then written out to a file that is read back in
     *         the first entry would indicate that it was set from the command line,
     *         while the second one would indicate the file that the new
     *         configuration was read in from.
     */
    public synchronized String[] getPropertySources(String name)
    {
        if (properties == null)
        {
            // If properties is null, it means a resource was newly added
            // but the props were cleared so as to load it upon future
            // requests. So lets force a load by asking a properties list.
            getProps();
        }
        // Return a null right away if our properties still
        // haven't loaded or the resource mapping isn't defined
        if (properties == null || updatingResource == null)
        {
            return null;
        }
        else
        {
            String[] source = updatingResource.get(name);
            if (source == null)
            {
                return null;
            }
            else
            {
                return Arrays.copyOf(source, source.length);
            }
        }
    }

    /**
     * A class that represents a set of positive integer ranges. It parses strings
     * of the form: "2-3,5,7-" where ranges are separated by comma and the
     * lower/upper bounds are separated by dash. Either the lower or upper bound may
     * be omitted meaning all values up to or over. So the string above means 2, 3,
     * 5, and 7, 8, 9, ...
     */
    public static class IntegerRanges implements Iterable<Integer>
    {
        private static class Range
        {
            int start;
            int end;
        }

        private static class RangeNumberIterator implements Iterator<Integer>
        {
            Iterator<Range> internal;
            int at;
            int end;

            public RangeNumberIterator(List<Range> ranges)
            {
                if (ranges != null)
                {
                    internal = ranges.iterator();
                }
                at = -1;
                end = -2;
            }

            @Override
            public boolean hasNext()
            {
                if (at <= end)
                {
                    return true;
                }
                else if (internal != null)
                {
                    return internal.hasNext();
                }
                return false;
            }

            @Override
            public Integer next()
            {
                if (at <= end)
                {
                    at++;
                    return at - 1;
                }
                else if (internal != null)
                {
                    Range found = internal.next();
                    if (found != null)
                    {
                        at = found.start;
                        end = found.end;
                        at++;
                        return at - 1;
                    }
                }
                return null;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        }

        ;

        List<Range> ranges = new ArrayList<Range>();

        public IntegerRanges()
        {
        }

        /**
         * 构造
         * 
         * @param newValue 值
         */
        public IntegerRanges(String newValue)
        {
            StringTokenizer itr = new StringTokenizer(newValue, ",");
            while (itr.hasMoreTokens())
            {
                String rng = itr.nextToken().trim();
                String[] parts = rng.split("-", 3);
                if (parts.length < 1 || parts.length > 2)
                {
                    throw new IllegalArgumentException("integer range badly formed: " + rng);
                }
                Range r = new Range();
                r.start = convertToInt(parts[0], 0);
                if (parts.length == 2)
                {
                    r.end = convertToInt(parts[1], Integer.MAX_VALUE);
                }
                else
                {
                    r.end = r.start;
                }
                if (r.start > r.end)
                {
                    throw new IllegalArgumentException("IntegerRange from " + r.start + " to " + r.end + " is invalid");
                }
                ranges.add(r);
            }
        }

        /**
         * Convert a string to an int treating empty strings as the default value.
         *
         * @param value the string value
         * @param defaultValue the value for if the string is empty
         * @return the desired integer
         */
        private static int convertToInt(String value, int defaultValue)
        {
            String trim = value.trim();
            if (trim.length() == 0)
            {
                return defaultValue;
            }
            return Integer.parseInt(trim);
        }

        /**
         * Is the given value in the set of ranges
         *
         * @param value the value to check
         * @return is the value in the ranges?
         */
        public boolean isIncluded(int value)
        {
            for (Range r : ranges)
            {
                if (r.start <= value && value <= r.end)
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * 是否为空
         * 
         * @return true if there are no values in this range, else false.
         */
        public boolean isEmpty()
        {
            return ranges == null || ranges.isEmpty();
        }

        @Override
        public String toString()
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Range r : ranges)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    result.append(',');
                }
                result.append(r.start);
                result.append('-');
                result.append(r.end);
            }
            return result.toString();
        }

        @Override
        public Iterator<Integer> iterator()
        {
            return new RangeNumberIterator(ranges);
        }

    }

    /**
     * Parse the given attribute as a set of integer ranges
     *
     * @param name the attribute name
     * @param defaultValue the default value if it is not set
     * @return a new set of ranges from the configured value
     */
    public IntegerRanges getRange(String name, String defaultValue)
    {
        return new IntegerRanges(get(name, defaultValue));
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as a
     * collection of <code>String</code>s. If no such property is specified then
     * empty collection is returned. This is an optimized version of
     * {@link #getStrings(String)}
     *
     * @param name property name.
     * @return property value as a collection of <code>String</code>s.
     */
    public Collection<String> getStringCollection(String name)
    {
        String valueString = get(name);
        return StringUtils.getStringCollection(valueString);
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as an array
     * of <code>String</code>s. If no such property is specified then
     * <code>null</code> is returned.
     *
     * @param name property name.
     * @return property value as an array of <code>String</code>s, or
     *         <code>null</code>.
     */
    public String[] getStrings(String name)
    {
        String valueString = get(name);
        return StringUtils.getStrings(valueString);
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as an array
     * of <code>String</code>s. If no such property is specified then default value
     * is returned.
     *
     * @param name property name.
     * @param defaultValue The default value
     * @return property value as an array of <code>String</code>s, or default value.
     */
    public String[] getStrings(String name, String... defaultValue)
    {
        String valueString = get(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        else
        {
            return StringUtils.getStrings(valueString);
        }
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as a
     * collection of <code>String</code>s, trimmed of the leading and trailing
     * whitespace. If no such property is specified then empty
     * <code>Collection</code> is returned.
     *
     * @param name property name.
     * @return property value as a collection of <code>String</code>s, or empty
     *         <code>Collection</code>
     */
    public Collection<String> getTrimmedStringCollection(String name)
    {
        String valueString = get(name);
        if (null == valueString)
        {
            Collection<String> empty = new ArrayList<String>();
            return empty;
        }
        return StringUtils.getTrimmedStringCollection(valueString);
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as an array
     * of <code>String</code>s, trimmed of the leading and trailing whitespace. If
     * no such property is specified then an empty array is returned.
     *
     * @param name property name.
     * @return property value as an array of trimmed <code>String</code>s, or empty
     *         array.
     */
    public String[] getTrimmedStrings(String name)
    {
        String valueString = get(name);
        return StringUtils.getTrimmedStrings(valueString);
    }

    /**
     * Get the comma delimited values of the <code>name</code> property as an array
     * of <code>String</code>s, trimmed of the leading and trailing whitespace. If
     * no such property is specified then default value is returned.
     *
     * @param name property name.
     * @param defaultValue The default value
     * @return property value as an array of trimmed <code>String</code>s, or
     *         default value.
     */
    public String[] getTrimmedStrings(String name, String... defaultValue)
    {
        String valueString = get(name);
        if (null == valueString)
        {
            return defaultValue;
        }
        else
        {
            return StringUtils.getTrimmedStrings(valueString);
        }
    }

    /**
     * Load a class by name.
     *
     * @param name the class name.
     * @return the class object.
     * @throws ClassNotFoundException if the class is not found.
     */
    public Class<?> getClassByName(String name) throws ClassNotFoundException
    {
        Class<?> ret = getClassByNameOrNull(name);
        if (ret == null)
        {
            throw new ClassNotFoundException("Class " + name + " not found");
        }
        return ret;
    }

    /**
     * Load a class by name, returning null rather than throwing an exception if it
     * couldn't be loaded. This is to avoid the overhead of creating an exception.
     *
     * @param name the class name
     * @return the class object, or null if it could not be found.
     */
    public Class<?> getClassByNameOrNull(String name)
    {
        Map<String, WeakReference<Class<?>>> map;

        synchronized (CACHE_CLASSES)
        {
            map = CACHE_CLASSES.get(classLoader);
            if (map == null)
            {
                map = Collections.synchronizedMap(new WeakHashMap<String, WeakReference<Class<?>>>());
                CACHE_CLASSES.put(classLoader, map);
            }
        }

        Class<?> clazz = null;
        WeakReference<Class<?>> ref = map.get(name);
        if (ref != null)
        {
            clazz = ref.get();
        }

        if (clazz == null)
        {
            try
            {
                clazz = Class.forName(name, true, classLoader);
            }
            catch (ClassNotFoundException e)
            {
                // Leave a marker that the class isn't found
                map.put(name, new WeakReference<Class<?>>(NEGATIVE_CACHE_SENTINEL));
                return null;
            }
            // two putters can race here, but they'll put the same class
            map.put(name, new WeakReference<Class<?>>(clazz));
            return clazz;
        }
        else if (clazz == NEGATIVE_CACHE_SENTINEL)
        {
            return null; // not found
        }
        else
        {
            // cache hit
            return clazz;
        }
    }

    /**
     * Get the value of the <code>name</code> property as an array of
     * <code>Class</code>. The value of the property specifies a list of comma
     * separated class names. If no such property is specified, then
     * <code>defaultValue</code> is returned.
     *
     * @param name the property name.
     * @param defaultValue default value.
     * @return property value as a <code>Class[]</code>, or
     *         <code>defaultValue</code>.
     */
    public Class<?>[] getClasses(String name, Class<?>... defaultValue)
    {
        String[] classnames = getTrimmedStrings(name);
        if (classnames == null)
        {
            return defaultValue;
        }
        try
        {
            Class<?>[] classes = new Class<?>[classnames.length];
            for (int i = 0; i < classnames.length; i++)
            {
                classes[i] = getClassByName(classnames[i]);
            }
            return classes;
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the value of the <code>name</code> property as a <code>Class</code>. If
     * no such property is specified, then <code>defaultValue</code> is returned.
     *
     * @param name the class name.
     * @param defaultValue default value.
     * @return property value as a <code>Class</code>, or <code>defaultValue</code>.
     */
    public Class<?> getClass(String name, Class<?> defaultValue)
    {
        String valueString = getTrimmed(name);
        if (valueString == null)
        {
            return defaultValue;
        }
        try
        {
            return getClassByName(valueString);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the value of the <code>name</code> property as a <code>Class</code>
     * implementing the interface specified by <code>xface</code>. If no such
     * property is specified, then <code>defaultValue</code> is returned. An
     * exception is thrown if the returned class does not implement the named
     * interface.
     *
     * @param <U> 泛型
     * @param name the class name.
     * @param defaultValue default value.
     * @param xface the interface implemented by the named class.
     * @return property value as a <code>Class</code>, or <code>defaultValue</code>.
     */
    public <U> Class<? extends U> getClass(String name, Class<? extends U> defaultValue, Class<U> xface)
    {
        try
        {
            Class<?> theClass = getClass(name, defaultValue);
            if (theClass != null && !xface.isAssignableFrom(theClass))
            {
                throw new RuntimeException(theClass + " not " + xface.getName());
            }
            else if (theClass != null)
            {
                return theClass.asSubclass(xface);
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a local file name under a directory named in <i>dirsProp</i> with the
     * given <i>path</i>. If <i>dirsProp</i> contains multiple directories, then one
     * is chosen based on <i>path</i>'s hash code. If the selected directory does
     * not exist, an attempt is made to create it.
     *
     * @param dirsProp directory in which to locate the file.
     * @param path file-path.
     * @return local file under the directory with the given path.
     * @throws IOException 异常
     */
    public File getFile(String dirsProp, String path) throws IOException
    {
        String[] dirs = getTrimmedStrings(dirsProp);
        int hashCode = path.hashCode();
        for (int i = 0; i < dirs.length; i++)
        {
            // try each local dir
            int index = (hashCode + i & Integer.MAX_VALUE) % dirs.length;
            File file = new File(dirs[index], path);
            File dir = file.getParentFile();
            if (dir.exists() || dir.mkdirs())
            {
                return file;
            }
        }
        throw new IOException("No valid local directories in property: " + dirsProp);
    }

    /**
     * Get the {@link URL} for the named resource.
     *
     * @param name resource name.
     * @return the url for the named resource.
     */
    public URL getResource(String name)
    {
        return classLoader.getResource(name);
    }

    /**
     * Get an input stream attached to the configuration resource with the given
     * <code>name</code>.
     *
     * @param name configuration resource name.
     * @return an input stream attached to the resource.
     */
    public InputStream getConfResourceAsInputStream(String name)
    {
        try
        {
            URL url = getResource(name);

            if (url == null)
            {
                LOG.info(name + " not found");
                return null;
            }
            else
            {
                LOG.info("found resource " + name + " at " + url);
            }

            return url.openStream();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get a {@link Reader} attached to the configuration resource with the given
     * <code>name</code>.
     *
     * @param name configuration resource name.
     * @return a reader attached to the resource.
     */
    public Reader getConfResourceAsReader(String name)
    {
        try
        {
            URL url = getResource(name);

            if (url == null)
            {
                LOG.info(name + " not found");
                return null;
            }
            else
            {
                LOG.info("found resource " + name + " at " + url);
            }

            return new InputStreamReader(url.openStream());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get the set of parameters marked final.
     *
     * @return final parameter set.
     */
    public Set<String> getFinalParameters()
    {
        return new HashSet<String>(finalParameters);
    }

    protected synchronized Properties getProps()
    {
        if (properties == null)
        {
            properties = new Properties();

            HashMap<String, String[]> backup = new HashMap<String, String[]>(updatingResource);

            loadResources(properties, resources, quietmode);
            if (overlay != null)
            {
                properties.putAll(overlay);
                for (Entry<Object, Object> item : overlay.entrySet())
                {
                    String key = (String) item.getKey();
                    updatingResource.put(key, backup.get(key));
                }
            }
        }
        return properties;
    }

    /**
     * Return the number of keys in the configuration.
     *
     * @return number of keys in the configuration.
     */
    public int size()
    {
        return getProps().size();
    }

    /**
     * Clears all keys from the configuration.
     */
    public void clear()
    {
        getProps().clear();
        getOverlay().clear();
    }

    /**
     * Get an {@link Iterator} to go through the list of <code>String</code>
     * key-value pairs in the configuration.
     *
     * @return an iterator over the entries.
     */
    @Override
    public Iterator<Entry<String, String>> iterator()
    {
        // Get a copy of just the string to string pairs. After the old object
        // methods that allow non-strings to be put into configurations are
        // removed,
        // we could replace properties with a Map<String,String> and get rid of
        // this
        // code.
        Map<String, String> result = new HashMap<String, String>();
        for (Entry<Object, Object> item : getProps().entrySet())
        {
            if (item.getKey() instanceof String && item.getValue() instanceof String)
            {
                result.put((String) item.getKey(), (String) item.getValue());
            }
        }
        return result.entrySet().iterator();
    }

    private Document parse(DocumentBuilder builder, URL url) throws IOException, SAXException
    {
        if (!quietmode)
        {
            LOG.debug("parsing URL " + url);
        }
        if (url == null)
        {
            return null;
        }
        return parse(builder, url.openStream(), url.toString());
    }

    private Document parse(DocumentBuilder builder, InputStream is, String systemId) throws IOException, SAXException
    {
        if (!quietmode)
        {
            LOG.debug("parsing input stream " + is);
        }
        if (is == null)
        {
            return null;
        }
        try
        {
            return (systemId == null) ? builder.parse(is) : builder.parse(is, systemId);
        }
        finally
        {
            is.close();
        }
    }

    private void loadResources(Properties properties, ArrayList<Resource> resources, boolean quiet)
    {
        if (loadDefaults)
        {
            for (String resource : DEFAULTRESOURCES)
            {
                loadResource(properties, new Resource(resource), quiet);
            }
        }

        for (int i = 0; i < resources.size(); i++)
        {
            Resource ret = loadResource(properties, resources.get(i), quiet);
            if (ret != null)
            {
                resources.set(i, ret);
            }
        }
    }

    private Resource loadResource(Properties properties, Resource wrapper, boolean quiet)
    {
        String name = UNKNOWN_RESOURCE;
        try
        {
            Object resource = wrapper.getResource();
            name = wrapper.getName();

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            // ignore all comments inside the xml file
            docBuilderFactory.setIgnoringComments(true);

            // allow includes in the xml file
            docBuilderFactory.setNamespaceAware(true);
            try
            {
                docBuilderFactory.setXIncludeAware(true);
            }
            catch (UnsupportedOperationException e)
            {
                LOG.error("Failed to set setXIncludeAware(true) for parser " + docBuilderFactory + ":" + e, e);
            }
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = null;
            Element root = null;
            boolean returnCachedProperties = false;

            if (resource instanceof URL)
            { // an URL resource
                doc = parse(builder, (URL) resource);
            }
            else if (resource instanceof String)
            {
                // a Local File PATH resource
                // Can't use FileSystem API or we get an infinite loop
                // since FileSystem uses Configuration API. Use java.io.File
                // instead.

                String srcFile = (String) resource;
                File file = new File(srcFile).getAbsoluteFile();
                if (file.exists())
                {
                    if (!quiet)
                    {
                        LOG.debug("parsing File " + file);
                    }
                    doc = parse(builder, new BufferedInputStream(new FileInputStream(file)), srcFile);
                }
            }
            else if (resource instanceof Path)
            {
                // a file resource
                // Can't use FileSystem API or we get an infinite loop
                // since FileSystem uses Configuration API. Use java.io.File
                // instead.
                File file = new File(((Path) resource).toUri().getPath()).getAbsoluteFile();
                if (file.exists())
                {
                    if (!quiet)
                    {
                        LOG.debug("parsing File " + file);
                    }
                    doc = parse(builder, new BufferedInputStream(new FileInputStream(file)),
                        ((Path) resource).toString());
                }
            }
            else if (resource instanceof InputStream)
            {
                doc = parse(builder, (InputStream) resource, null);
                returnCachedProperties = true;
            }
            else if (resource instanceof Properties)
            {
                overlay(properties, (Properties) resource);
            }
            else if (resource instanceof Element)
            {
                root = (Element) resource;
            }

            if (root == null)
            {
                if (doc == null)
                {
                    if (quiet)
                    {
                        return null;
                    }
                    throw new RuntimeException(resource + " not found");
                }
                root = doc.getDocumentElement();
            }
            Properties toAddTo = properties;
            if (returnCachedProperties)
            {
                toAddTo = new Properties();
            }
            if (!"configuration".equals(root.getTagName()))
            {
                LOG.error("bad conf file: top-level element not <configuration>");
            }
            NodeList props = root.getChildNodes();
            DeprecationContext deprecations = deprecationContext.get();
            for (int i = 0; i < props.getLength(); i++)
            {
                Node propNode = props.item(i);
                if (!(propNode instanceof Element))
                {
                    continue;
                }
                Element prop = (Element) propNode;
                if ("configuration".equals(prop.getTagName()))
                {
                    loadResource(toAddTo, new Resource(prop, name), quiet);
                    continue;
                }
                if (!"property".equals(prop.getTagName()))
                {
                    LOG.warn("bad conf file: element not <property>");
                }
                NodeList fields = prop.getChildNodes();
                String attr = null;
                String value = null;
                boolean finalParameter = false;
                LinkedList<String> source = new LinkedList<String>();
                for (int j = 0; j < fields.getLength(); j++)
                {
                    Node fieldNode = fields.item(j);
                    if (!(fieldNode instanceof Element))
                    {
                        continue;
                    }
                    Element field = (Element) fieldNode;
                    if ("name".equals(field.getTagName()) && field.hasChildNodes())
                    {
                        attr = StringInterner.weakIntern(((Text) field.getFirstChild()).getData().trim());
                    }
                    if ("value".equals(field.getTagName()) && field.hasChildNodes())
                    {
                        value = StringInterner.weakIntern(((Text) field.getFirstChild()).getData());
                    }
                    if ("final".equals(field.getTagName()) && field.hasChildNodes())
                    {
                        finalParameter = "true".equals(((Text) field.getFirstChild()).getData());
                    }
                    if ("source".equals(field.getTagName()) && field.hasChildNodes())
                    {
                        source.add(StringInterner.weakIntern(((Text) field.getFirstChild()).getData()));
                    }
                }
                source.add(name);

                // Ignore this parameter if it has already been marked as
                // 'final'
                if (attr != null)
                {
                    if (deprecations.getDeprecatedKeyMap().containsKey(attr))
                    {
                        DeprecatedKeyInfo keyInfo = deprecations.getDeprecatedKeyMap().get(attr);
                        keyInfo.clearAccessed();
                        for (String key : keyInfo.newKeys)
                        {
                            // update new keys with deprecated key's value
                            loadProperty(toAddTo, name, key, value, finalParameter,
                                source.toArray(new String[source.size()]));
                        }
                    }
                    else
                    {
                        loadProperty(toAddTo, name, attr, value, finalParameter,
                            source.toArray(new String[source.size()]));
                    }
                }
            }

            if (returnCachedProperties)
            {
                overlay(properties, toAddTo);
                return new Resource(toAddTo, name);
            }
            return null;
        }
        catch (IOException e)
        {
            LOG.error("error parsing conf " + name, e);
            throw new RuntimeException(e);
        }
        catch (DOMException e)
        {
            LOG.error("error parsing conf " + name, e);
            throw new RuntimeException(e);
        }
        catch (SAXException e)
        {
            LOG.error("error parsing conf " + name, e);
            throw new RuntimeException(e);
        }
        catch (ParserConfigurationException e)
        {
            LOG.error("error parsing conf " + name, e);
            throw new RuntimeException(e);
        }
    }

    private void overlay(Properties to, Properties from)
    {
        for (Entry<Object, Object> entry : from.entrySet())
        {
            to.put(entry.getKey(), entry.getValue());
        }
    }

    private void loadProperty(Properties properties, String name, String attr, String value, boolean finalParameter,
        String[] source)
    {
        if (value != null)
        {
            if (!finalParameters.contains(attr))
            {
                properties.setProperty(attr, value);
                updatingResource.put(attr, source);
            }
            else if (!value.equals(properties.getProperty(attr)))
            {
                LOG.warn(name + ":an attempt to override final parameter: " + attr + ";  Ignoring.");
            }
        }
        if (finalParameter)
        {
            finalParameters.add(attr);
        }
    }

    /**
     * Write out the non-default properties in this configuration to the given
     * {@link OutputStream} using UTF-8 encoding.
     *
     * @param out the output stream to write to.
     * @throws IOException 异常
     */
    public void writeXml(OutputStream out) throws IOException
    {
        writeXml(new OutputStreamWriter(out, "UTF-8"));
    }

    /**
     * Write out the non-default properties in this configuration to the given
     * {@link Writer}.
     *
     * @param out the writer to write to.
     * @throws IOException 异常
     */
    public void writeXml(Writer out) throws IOException
    {
        Document doc = asXmlDocument();

        try
        {
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(out);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();

            // Important to not hold Configuration log while writing result,
            // since
            // 'out' may be an HDFS stream which needs to lock this
            // configuration
            // from another thread.
            transformer.transform(source, result);
        }
        catch (TransformerException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Return the XML DOM corresponding to this Configuration.
     */
    private synchronized Document asXmlDocument() throws IOException
    {
        Document doc;
        try
        {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }
        catch (ParserConfigurationException e)
        {
            throw new IOException(e);
        }
        Element conf = doc.createElement("configuration");
        doc.appendChild(conf);
        conf.appendChild(doc.createTextNode("\n"));
        // ensure properties is set and deprecation is handled
        handleDeprecation();
        for (Enumeration<Object> e = properties.keys(); e.hasMoreElements();)
        {
            String name = (String) e.nextElement();
            Object object = properties.get(name);
            String value = null;
            if (object instanceof String)
            {
                value = (String) object;
            }
            else
            {
                continue;
            }
            Element propNode = doc.createElement("property");
            conf.appendChild(propNode);

            Element nameNode = doc.createElement("name");
            nameNode.appendChild(doc.createTextNode(name));
            propNode.appendChild(nameNode);

            Element valueNode = doc.createElement("value");
            valueNode.appendChild(doc.createTextNode(value));
            propNode.appendChild(valueNode);

            if (updatingResource != null)
            {
                String[] sources = updatingResource.get(name);
                if (sources != null)
                {
                    for (String s : sources)
                    {
                        Element sourceNode = doc.createElement("source");
                        sourceNode.appendChild(doc.createTextNode(s));
                        propNode.appendChild(sourceNode);
                    }
                }
            }

            conf.appendChild(doc.createTextNode("\n"));
        }
        return doc;
    }

    /**
     * Get the {@link ClassLoader} for this job.
     *
     * @return the correct class loader.
     */
    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    /**
     * Set the class loader that will be used to load the various objects.
     *
     * @param classLoader the new class loader.
     */
    public void setClassLoader(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration: ");
        if (loadDefaults)
        {
            toString(DEFAULTRESOURCES, sb);
            if (resources.size() > 0)
            {
                sb.append(", ");
            }
        }
        toString(resources, sb);
        return sb.toString();
    }

    private <T> void toString(List<T> resources, StringBuilder sb)
    {
        ListIterator<T> i = resources.listIterator();
        while (i.hasNext())
        {
            if (i.nextIndex() != 0)
            {
                sb.append(", ");
            }
            sb.append(i.next());
        }
    }

    /**
     * Set the quietness-mode. In the quiet-mode, error and informational messages
     * might not be logged.
     *
     * @param quietmode <code>true</code> to set quiet-mode on, <code>false</code>
     *            to turn it off.
     */
    public synchronized void setQuietMode(boolean quietmode)
    {
        this.quietmode = quietmode;
    }

    synchronized boolean getQuietMode()
    {
        return this.quietmode;
    }

    /**
     * get keys matching the the regex
     *
     * @param regex 正则
     * @return map with matching keys
     */
    public Map<String, String> getValByRegex(String regex)
    {
        Pattern p = Pattern.compile(regex);

        Map<String, String> result = new HashMap<String, String>();
        Matcher m;

        for (Entry<Object, Object> item : getProps().entrySet())
        {
            if (item.getKey() instanceof String && item.getValue() instanceof String)
            {
                m = p.matcher((String) item.getKey());
                if (m.find())
                { // match
                    result.put((String) item.getKey(), substituteVars(getProps().getProperty((String) item.getKey())));
                }
            }
        }
        return result;
    }

    /**
     * A unique class which is used as a sentinel value in the caching for
     * getClassByName. {@see Configuration#getClassByNameOrNull(String)}
     */
    private abstract static class NegativeCacheSentinel
    {
    }

    /**
     * Returns whether or not a deprecated name has been warned. If the name is not
     * deprecated then always return false
     * 
     * @param name 名称
     * @return 有无警告
     */
    public static boolean hasWarnedDeprecation(String name)
    {
        DeprecationContext deprecations = deprecationContext.get();
        if (deprecations.getDeprecatedKeyMap().containsKey(name))
        {
            if (deprecations.getDeprecatedKeyMap().get(name).accessed.get())
            {
                return true;
            }
        }
        return false;
    }

    /***
     * <p>
     * 获取Local FileSystem路径对象
     * </p>
     * 
     * @param pathName 路径名
     * @return 路径对象
     */
    private Path getPathObject(String pathName)
    {
        FileSystem fs = FileSystems.getDefault();
        File file = new File(pathName);
        String newPath = pathName;
        if (!file.exists())
        {
            newPath = "conf/" + pathName;
        }
        file = new File(newPath);
        if (!file.exists())
        {
            file = new File(BaseConfiguration.class.getClassLoader().getResource("").getPath());
            file = new File(file.getParent() + "/" + newPath);
        }

        if (file.exists())
        {
            return fs.getPath(file.getPath());
        }
        else
        {
            file = new File(pathName);
            if (!file.exists())
            {
                file = new File(BaseConfiguration.class.getClassLoader().getResource("").getPath());
                file = new File(file.getParent() + "/" + pathName);

                if (!file.exists())
                {
                    throw new RuntimeException("can not find file path:" + pathName);
                }
            }

            return fs.getPath(file.getPath());
        }
    }
}
