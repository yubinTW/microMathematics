/*******************************************************************************
 * microMathematics Plus - Extended visual calculator
 * *****************************************************************************
 * Copyright (C) 2014-2017 Mikhail Kulesh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.mkulesh.micromath.formula;

import android.content.Context;
import android.content.res.Resources;
import android.widget.LinearLayout;

import com.mkulesh.micromath.math.CalculatedValue;
import com.mkulesh.micromath.plus.R;
import com.mkulesh.micromath.utils.CompatUtils;
import com.mkulesh.micromath.widgets.CustomEditText;
import com.mkulesh.micromath.widgets.CustomTextView;

public abstract class FormulaTermFunctionBase extends FormulaTerm
{
    /**
     * Error codes that can be generated by function term
     */
    enum ErrorCode
    {
        NO_ERROR(-1),
        UNKNOWN_FUNCTION(R.string.error_unknown_function),
        UNKNOWN_ARRAY(R.string.error_unknown_array),
        NOT_AN_ARRAY(R.string.error_not_an_array),
        NOT_A_FUNCTION(R.string.error_not_a_function),
        RECURSIVE_CALL(R.string.error_recursive_call),
        NOT_DIFFERENTIABLE(R.string.error_not_differentiable);

        private final int descriptionId;

        ErrorCode(int descriptionId)
        {
            this.descriptionId = descriptionId;
        }

        public String getDescription(Context context)
        {
            return context.getResources().getString(descriptionId);
        }
    }

    protected CustomTextView functionTerm = null;
    protected CalculatedValue[] argVal = null;

    /*********************************************************
     * Constructors
     *********************************************************/

    public FormulaTermFunctionBase(TermField owner, LinearLayout layout) throws Exception
    {
        super(owner.getFormulaRoot(), layout, owner.termDepth);
        setParentField(owner);
    }

    public FormulaTermFunctionBase()
    {
        super();
    }

    /*********************************************************
     * Methods to be Implemented in derived a class
     *********************************************************/

    /**
     * Returns function label to be used if function is deleted
     */
    protected abstract String getFunctionLabel();

    /*********************************************************
     * Helper methods used by derived class
     *********************************************************/

    public static boolean containsGeneralTrigger(Context context, String s)
    {
        return s.contains(context.getResources().getString(R.string.formula_function_start_bracket));
    }

    public CustomTextView getFunctionTerm()
    {
        return functionTerm;
    }

    protected void setErrorCode(ErrorCode errorCode, String addInfo)
    {
        if (functionTerm != null)
        {
            functionTerm.setTextColor(CompatUtils.getThemeColorAttr(getContext(), R.attr.colorFormulaNormal));
        }
        if (parentField != null)
        {
            String errorMsg = null;
            switch (errorCode)
            {
            case NO_ERROR:
                // nothing to do
                break;
            case UNKNOWN_FUNCTION:
            case UNKNOWN_ARRAY:
            case NOT_AN_ARRAY:
            case NOT_A_FUNCTION:
            case NOT_DIFFERENTIABLE:
                errorMsg = String.format(errorCode.getDescription(getContext()), addInfo);
                break;
            case RECURSIVE_CALL:
                errorMsg = errorCode.getDescription(getContext());
                break;
            }
            parentField.setError(errorMsg, TermField.ErrorNotification.PARENT_LAYOUT, functionMainLayout);
        }
    }

    protected void ensureArgValSize()
    {
        final int termsSize = terms.size();
        if (argVal == null || argVal.length != termsSize)
        {
            argVal = new CalculatedValue[termsSize];
            for (int i = 0; i < termsSize; i++)
            {
                argVal[i] = new CalculatedValue();
            }
        }
    }

    @Override
    protected CustomTextView initializeSymbol(CustomTextView v)
    {
        final Resources res = getContext().getResources();
        if (v.getText() != null)
        {
            String t = v.getText().toString();
            if (t.equals(res.getString(R.string.formula_operator_key)))
            {
                v.prepare(CustomTextView.SymbolType.TEXT,
                        getFormulaRoot().getFormulaList().getActivity(), this);
                v.setText(getTermCode());
                functionTerm = v;
            }
            else if (t.equals(res.getString(R.string.formula_left_bracket_key)))
            {
                v.prepare(CustomTextView.SymbolType.LEFT_BRACKET,
                        getFormulaRoot().getFormulaList().getActivity(), this);
                v.setText("."); // this text defines view width/height
            }
            else if (t.equals(res.getString(R.string.formula_right_bracket_key)))
            {
                v.prepare(CustomTextView.SymbolType.RIGHT_BRACKET,
                        getFormulaRoot().getFormulaList().getActivity(), this);
                v.setText("."); // this text defines view width/height
            }
        }
        return v;
    }

    protected boolean isRemainingTermOnDelete()
    {
        return terms.size() <= 1;
    }

    @Override
    public void onDelete(CustomEditText owner)
    {
        final TermField ownerTerm = findTerm(owner);

        if (owner == null || isRemainingTermOnDelete())
        {
            // search remaining text or term
            TermField remainingTerm = null;
            CharSequence remainingText = "";
            if (ownerTerm != null)
            {
                if (functionTerm != null)
                {
                    remainingText = getFunctionLabel();
                }
                for (TermField t : terms)
                {
                    if (t == ownerTerm)
                    {
                        continue;
                    }
                    if (t.isTerm())
                    {
                        remainingTerm = t;
                    }
                    else if (!t.isEmpty())
                    {
                        remainingText = t.getText();
                    }
                }
            }
            if (parentField != null && remainingTerm != null)
            {
                parentField.onTermDelete(removeElements(), remainingTerm);
            }
            else if (parentField != null)
            {
                parentField.onTermDeleteWithText(removeElements(), remainingText);
            }
            else
            {
                super.onDelete(null);
            }
        }
        else if (isNewTermEnabled())
        {
            if (parentField == null || ownerTerm == null)
            {
                return;
            }

            TermField prevTerm = deleteArgument(ownerTerm,
                    getContext().getResources().getString(R.string.formula_term_separator), /*storeUndoState=*/true);

            getFormulaRoot().getFormulaList().onManualInput();
            if (prevTerm != null)
            {
                prevTerm.requestFocus();
            }
        }
    }
}