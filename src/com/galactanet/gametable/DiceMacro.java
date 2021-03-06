/*
 * DiceMacro.java: GameTable is in the Public Domain.
 */


package com.galactanet.gametable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import com.galactanet.gametable.util.UtilityFunctions;
import com.galactanet.gametable.util.XmlSerializer;

/**
 * Encapsulates a dice macro.
 * 
 * @author sephalon
 * @author iffy
 */
public class DiceMacro
{
    // --- Types ---

    /**
     * A class to hold the results of a roll.
     * 
     * @author iffy
     */
    public static class Result
    {
        /**
         * The string of rolls made.
         */
        public String[] result;

        /**
         * The macro string executed.
         */
        public String roll;

        /**
         * The actual resulting value.
         */
        public int[] value;        
        
        /**
         * Convenience constructor.
         * 
         * @param val Actual result value.
         * @param rollText Macro string executed.
         * @param resultText Result string.
         */
        public Result(final int[] val, final String rollText, final String[] resultText)
        {
            value = val;
            roll = rollText;
            result = resultText;
        }
    }

    private static class TermList
    {
        /**
         * Number of times to Repear the Dice Roll
         */
        public int m_repeat = 1;
        public String m_termString = null;
        public final List<Term> m_terms = new ArrayList<Term>();
        
        public TermList() {            
        }
        
        public void add(Term oadd) {
            m_terms.add(oadd);
        }
// never used locally        
//        public Object get(final int i) {
//            return m_terms.get(i);
//        }
        public String toString() {
            return m_termString;
        }
        public void setString(final String s) {
            m_termString = s;
        }
        
    }
    
    /**
     * An encapsulation of one term of the DiceMacro.
     * 
     * @author sephalon
     */    
    private class Term
    {
        /**
         * The number of sides on this die. A "1" sided die is a bonus.
         */
        public int m_die;

        /**
         * The number of highest rolls to keep. If negative, keeps the lowest rolls. A value of 0 means to keep all
         * rolls.
         */
        public int m_keep;

        /**
         * Number of die rolls for this term. If negative, then the roll result is negated. If a bonus, than this is
         * just the bonus value.
         */
        public int m_qty;
       
        /**
         * @param qty
         * @param die
         * @param keep
         */
        public Term(final int qty, final int die, final int keep)
        {
            m_qty = qty;
            m_die = die;
            m_keep = keep;
        }

        /*
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object o)
        {
            try
            {
                final Term t = (Term)o;
                return (m_die == t.m_die) && (m_qty == t.m_qty) && (m_keep == t.m_keep);
            }
            catch (final ClassCastException cce)
            {
                return false;
            }
        }

        /*
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            final StringBuffer result = new StringBuffer();

            if (m_die > 1)
            {
                if ((m_qty < -1) || (m_qty > 1))
                {
                    result.append(m_qty);
                }
                else if (m_qty == -1)
                {
                    result.append('-');
                }

                result.append('d');
                result.append(m_die);
                if (m_keep < 0)
                {
                    result.append('l');
                    result.append(0 - m_keep);
                }
                else if (m_keep > 0)
                {
                    result.append('h');
                    result.append(m_keep);
                }
            }
            else
            {
                result.append(m_qty);
            }

            return result.toString();
        }
    }
    
    private StringBuffer outputString = null;
  
    public String generateOutputString(final String rollerName, final List<Result> result, final String rollName)
    {        
        final StringBuffer ret = new StringBuffer();
        if(rollerName == null) ret.append("You Privately Roll ");
        else ret.append(UtilityFunctions.emitUserLink(rollerName) + " rolls ");
        
        outputString = new StringBuffer();
        outputString.append("<b><font color=\"#006600\">");  
        outputString.append(rollName + "</font></b>:");        
        for(int i = 0;i < result.size();i++) {
            final Result curResult = result.get(i);
            if(i > 0) outputString.append(",");
            outputString.append(" (" + curResult.roll +  ") : [");
            for(int j = 0;j < curResult.result.length; j++) {
                if(j != 0) outputString.append(",");
                outputString.append(curResult.result[j]);
            }
            outputString.append("] = " + GametableFrame.DIEROLL_MESSAGE_FONT);
            for(int j = 0;j < curResult.value.length; j++) {
                if(j != 0) outputString.append(",");
                outputString.append(curResult.value[j]);
            }   
            outputString.append(GametableFrame.END_DIEROLL_MESSAGE_FONT);
        }
        
        ret.append(outputString);
        return ret.toString();
    }
   
    
    /**
     * Compares the normalized values of the two Strings.
     * 
     * @param a First String to compare.
     * @param b Second String to compare.
     * @return True if the two strings are the same after normalization.
     */
    private static boolean isSameMacroString(final String a, final String b)
    {
        if (a == b)
        {
            return true;
        }

        if ((a == null) || (b == null))
        {
            return false;
        }

        return normalizeMacroString(a).equals(normalizeMacroString(b));
    }

    /**
     * Regularlizes a string before parsing.
     * 
     * @param in Macro string to normalize.
     * @return Normalized macro string.
     */
    private static String normalizeMacroString(final String in)
    {
        // Remove spaces.
        final StringBuffer buffer = new StringBuffer();
        for (int index = 0; index < in.length(); index++)
        {
            if (!Character.isWhitespace(in.charAt(index)))
            {
                buffer.append(Character.toLowerCase(in.charAt(index)));
            }
        }

        return buffer.toString();
    }

    /**
     * Rolls a die with the given number of sides.
     * 
     * @param sides Sides of the die to roll.
     * @return The result of the die roll.
     */
    private static int rollDie(final int sides)
    {
        return UtilityFunctions.getRandom(sides) + 1;
    }

    // --- Members ---

    /**
     * The parsed and then reserialized macro text that initialized this macro.
     */
    private String     m_macro = "0";

    /**
     * The name of this macro. Null means an "anonymous" macro.
     */
    private String     m_name  = null;

    /**
     * The name of this macro. Null means an "anonymous" macro.
     */
    private String     m_parent  = null;

   /**
     * List of parsed terms in this macro.
     */
    private final List<TermList> m_terms = new ArrayList<TermList>();

    // --- Constructors ---

    /**
     * Uninitialized Constructor.
     */
    public DiceMacro()
    {
    }

    /**
     * Initialized constructor.
     */
    public DiceMacro(final String macro, final String name, final String parent)
    {
        if (!init(macro, name, parent))
        {
            reset();
        }
    }

    public DiceMacro(final String macro) {
        if(!init(macro, macro, null)) {
            reset();
        }
    }
    // --- Methods ---

    /**
     * Executes this macro and returns the formatted result.
     * 
     * @return The formatted result string.
     */
    public void doMacro(final boolean priv)
    {
        if (!isInitialized()) return;
        
        final Player me = GametableFrame.getGametableFrame().getMyPlayer();
        final String name = me.getCharacterName();

        final List<Result> result = roll();

        if(priv)
            GametableFrame.getGametableFrame().getChatPanel().logMechanics(generateOutputString(null, result, getName()));
        else
            GametableFrame.getGametableFrame().postMechanics(generateOutputString(name, result, getName()));
        
    }
    
    public List<Result> runMacro() {
        return roll();
    }

    /**
     * @return Returns the macro.
     */
    public String getMacro()
    {
        return m_macro;
    }

    /* 
    /**
     * Serializes the parsed Macro terms back into a source string that would generate the same macro.
     * 
     * @return The serialized macro string.
     */
    /* private String getMacroString()
    {
        if (!isInitialized())
        {
            return "";
        }

        final StringBuffer buffer = new StringBuffer();
        boolean bIsFirst = true;

        for (int i = 0; i < m_terms.size(); i++)
        {
            final String dice = m_terms.get(i).toString();
            if (!bIsFirst)
            {
                buffer.append(' ');
            }

            final boolean negate = (dice.charAt(0) == '-');
            if (negate)
            {
                buffer.append("- ");
                buffer.append(dice.substring(1).trim());
            }
            else
            {
                if (!bIsFirst)
                {
                    buffer.append("+ ");
                }

                buffer.append(dice);
            }
            bIsFirst = false;
        }

        return buffer.toString();
    }

    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * @return Returns the parent group.
     */
    public String getParent()
    {
        return m_parent;
    }

    /*
     * @return The string used to describe this macro when formatting a roll.
     *
    private String getRollString()
    {
        if (!isInitialized())
        {
            return "";
        }

        return toFormattedString();
    }
    */
    
    /**
     * Initializes the Macro with the given data.
     * 
     * @param macro Macro string to parse.
     * @param name Name of this macro. Null means "anonymous". Empty string means use the parsed macro as the name.
     * @return True if the macro string was parseable.
     */
    public boolean init(final String macro, final String name, final String parent)
    {
        reset();
        try
        {
            setName(name);
            setParent(parent);
            setMacro(normalizeMacroString(macro));

            // Parse the macro string. It will be something like
            // "3d6 + 4" or "2d4 + 3d6h2 + 8"

            if (!isInitialized())
            {
                return true;
            }
            
            StringTokenizer strtok = new StringTokenizer(getMacro(),";");            
            while(strtok.hasMoreTokens()) {                
                final TermList cur_terms = new TermList();
                String thisTerm = strtok.nextToken();
                cur_terms.setString(thisTerm);
                final int x = thisTerm.indexOf("x");
                if(x == 0) {
                  return false;  
                } else if(x > 0) {                
                    final String numberStr = thisTerm.substring(x+1);
                    cur_terms.m_repeat = Integer.parseInt(numberStr);  
                    thisTerm = thisTerm.substring(0,x);                    
                }
                
                // Grab individual dice rolls.
                boolean isNegative = false;
                //  #dice, type, #keep
                final int[] numbers = {
                    1, 1, 0, 1
                };

                //  Corresponds with index of numbers array
                int phase = 0;
                int startOfCurrentNumber = 0;
                final int length = thisTerm.length();
                for (int index = 0; index < length; ++index)
                {
                    final char c = thisTerm.charAt(index);
                    final boolean isLast = (index == (length - 1));
                    if (!Character.isDigit(c) || isLast)
                    {
                        //  End of this number.
                        if ((startOfCurrentNumber != index) || isLast)
                        {
                            // end is position after number.
                            final int end = (isLast ? length : index);
                            final String numberStr = thisTerm.substring(startOfCurrentNumber, end);
                            int number = Integer.parseInt(numberStr);                            
                            numbers[phase] = isNegative ? (-number) : number;
                            isNegative = false;
                        }
                        startOfCurrentNumber = index + 1;
                        
                        // Check for end of dice roll.
                        if ((c == '+') || (c == '-') || isLast)
                        {
                            if ((index > 0) || isLast)
                            {
                                cur_terms.add(new Term(numbers[0], numbers[1], numbers[2]));
                            }
                            isNegative = (c == '-');
                            numbers[0] = 1;
                            numbers[1] = 1;
                            numbers[2] = 0;
                            numbers[3] = 1;
                            phase = 0;
                        }
                        else if (c == 'd')
                        {
                            numbers[0] = isNegative ? (-numbers[0]) : numbers[0];
                            isNegative = false;
                            phase = 1;
                        }
                        else if (c == 'h')
                        {
                            isNegative = false;
                            phase = 2;
                        }
                        else if (c == 'l')
                        {
                            isNegative = true;
                            phase = 2;
                        }
                        else
                        {
                            reset();
                            return false;
                        }
                        
                    } 
                }
                m_terms.add(cur_terms);
            } 

            // setMacro(getMacroString());
            if ((getName() != null) && (getName().length() < 1))
            {
                setName(getMacro());
            }

            return true;
        }
        catch (final Throwable ex)
        {
            Log.log(Log.SYS, "parse error: \"" + getMacro() + "\" (\"" + macro + "\")");
            Log.log(Log.SYS, ex);
            reset();
            return false;
        }
    }

    public void initFromStream(final DataInputStream dis) throws IOException
    {
        final String name = dis.readUTF();
        final String macro = dis.readUTF();
        final String parent = dis.readUTF();
        init(macro, name, parent);
    }

    /**
     * @return True if this DiceMacro is initialized.
     */
    public boolean isInitialized()
    {
        return !("0".equals(getMacro()));
    }

    /**
     * Resets this dice macro to be uninitialized.
     */
    public void reset()
    {
        setName(null);
        setMacro("0");
        m_terms.clear();
    }

    /**
     * Executes this macro, returning a data object with various information about the result.
     * 
     * @return DiceMacro.Result object.
     */
    public List<Result> roll()
    {
        List<Result> rList = new ArrayList<Result>();
        if (!isInitialized())
        {
            return null;
        }
        
       
        for (int t = 0; t < m_terms.size(); t++) {
            TermList cur_term = m_terms.get(t);
            int[] totals = new int[cur_term.m_repeat];
            String[] results = new String[cur_term.m_repeat]; 
            
            for (int r = 0; r < cur_term.m_repeat; r++) {
                final StringBuffer ret = new StringBuffer(); 
                int total = 0;
                for (int i = 0; i < cur_term.m_terms.size(); i++) {
                    // get the die type
                    final Term dieType = cur_term.m_terms.get(i);
                    boolean bFirstAdd = (i == 0);

                    int subtotal = 0;
                    // special case: straight bonus
                    if (dieType.m_die < 2)
                    {
                        if (dieType.m_qty > 0)
                        {
                            if (!bFirstAdd)
                            {
                                ret.append(" + ");
                            }
                            ret.append(dieType.m_qty);
                            subtotal += dieType.m_qty;
                        }
                        else if (dieType.m_qty < 0)
                        {
                            if (!bFirstAdd)
                            {
                                ret.append(' ');
                            }
                            ret.append("- ");
                            ret.append(-dieType.m_qty);
                            subtotal += dieType.m_qty;
                        }
                    }
                    else if (dieType.m_keep == 0)
                    {
                        // normal case: die rollin!
                        // Straight addition
                        final int bound = Math.abs(dieType.m_qty);
                        final boolean negative = (dieType.m_qty < 0);
                        if (bound > 0)
                        {
                            // single quantity - no parentheticals
                            if (bound == 1)
                            {
                                final int value = rollDie(dieType.m_die);
                                if (bFirstAdd)
                                {
                                    if (negative)
                                    {
                                        ret.append("- ");
                                    }
                                }
                                else
                                {
                                    ret.append(negative ? " - " : " + ");
                                }

                                ret.append(value);
                                if (negative)
                                {
                                    subtotal -= value;
                                }
                                else
                                {
                                    subtotal += value;
                                }
                            }
                            else
                            {
                                // multiple quantity - parentheticals
                                if (bFirstAdd)
                                {
                                    if (negative)
                                    {
                                        ret.append("- ");
                                    }
                                }
                                else
                                {
                                    ret.append(negative ? " - " : " + ");
                                }

                                if (m_terms.size() > 1)
                                {
                                    ret.append('(');
                                }
                                for (int j = 0; j < bound; j++)
                                {
                                    final int value = rollDie(dieType.m_die);
                                    if (j > 0)
                                    {
                                        ret.append(" + ");
                                    }
                                    ret.append(value);
                                    if (negative)
                                    {
                                        subtotal -= value;
                                    }
                                    else
                                    {
                                        subtotal += value;
                                    }
                                }
                                if (m_terms.size() > 1)
                                {
                                    ret.append(')');
                                }
                            }
                        }
                    }
                    else
                    {
                        // Keep some rolls.
                        final int number = Math.abs(dieType.m_qty);
                        if (!bFirstAdd)
                        {
                            ret.append((dieType.m_qty < 0) ? " - " : " + ");
                        }
                        ret.append('(');
                        final int[] rolls = new int[number];
                        for (int index = 0; index < number; index++)
                        {
                            rolls[index] = rollDie(dieType.m_die);
                            if (index < 1)
                            {
                                ret.append(rolls[index]);
                            }
                            else
                            {
                                ret.append(',');
                                ret.append(rolls[index]);
                            }
                        }
                        Arrays.sort(rolls);
                        if (dieType.m_keep > 0)
                        {
                            // Keep highest
                            for (int index = dieType.m_qty - dieType.m_keep; index < dieType.m_qty; index++)
                            {
                                if (dieType.m_qty < 0)
                                {
                                    subtotal -= rolls[index];
                                }
                                else
                                {
                                    subtotal += rolls[index];
                                }
                            }
                            ret.append(")h");
                            ret.append(dieType.m_keep);
                        }
                        else
                        {
                            // Keep lowest
                            for (int index = 0; index < -dieType.m_keep; index++)
                            {
                                if (dieType.m_qty < 0)
                                {
                                    subtotal -= rolls[index];
                                }
                                else
                                {
                                    subtotal += rolls[index];
                                }
                            }
                            ret.append(")l");
                            ret.append(-dieType.m_keep);
                        }

                    }
                    total += subtotal;
                    // System.out.println(dieType + ": " + subtotal + " (" + total + ")");
                }
                totals[r] = total;
                results[r] = ret.toString();                
            }
            if(cur_term.m_repeat > 1) {
                Arrays.sort(totals); // Sort them in ascending order
                totals = UtilityFunctions.ArrayReverse(totals);
            }
            rList.add(new Result(totals, cur_term.toString(), results));            
        }
        return rList;        
    }

    public void sendTo(final String who) {
        final String fromName = GametableFrame.getGametableFrame().getMyPlayer().getCharacterName(); 
        final String msg = UtilityFunctions.emitUserLink(fromName) + " privately rolled: " + outputString.toString();                
        GametableFrame.getGametableFrame().postPrivMechanics(who, msg);
    }
    
    public void serialize(final XmlSerializer out) throws IOException
    {
        out.startElement(DiceMacroSaxHandler.ELEMENT_DICE_MACRO);
        out.addAttribute(DiceMacroSaxHandler.ATTRIBUTE_NAME, getName());
        out.addAttribute(DiceMacroSaxHandler.ATTRIBUTE_DEFINITION, getMacro());
        out.endElement();
    }

    /**
     * @param macro The macro to set.
     */
    private void setMacro(final String macro)
    {
        m_macro = macro;
    }

    /**
     * @param name The name to set.
     */
    private void setName(final String name)
    {
        m_name = name;
    }

    /**
     * @param name The name to set.
     */
    private void setParent(final String parent)
    {
        m_parent = parent;
    }

    public String toFormattedString()
    {
        if ((getName() == null) || isSameMacroString(getName(), getMacro()))
        {
            return "<b><font color=\"#006600\">" + getMacro() + "</font></b>";
        }

        return "<b><font color=\"#006600\">" + getName() + "</font></b>" + " (" + getMacro() + ")";
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if ((getName() == null) || isSameMacroString(getName(), getMacro()))
        {
            return getMacro();
        }

        return getName() + " (" + getMacro() + ")";
    }

    public void writeToStream(final DataOutputStream dos) throws IOException
    {
        dos.writeUTF(getName());
        dos.writeUTF(getMacro());
    }

}
