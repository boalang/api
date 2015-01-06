using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace edu.iastate.cs.boa
{
    public class BoaException : Exception
    {
        public BoaException() : base()
        { }

        public BoaException(String msg) : base(msg)
        { }

        public BoaException(String msg, Exception e) : base(msg, e)
        { }
    }
}
