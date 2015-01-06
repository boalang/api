using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace edu.iastate.cs.boa
{
    public class LoginException : BoaException
    {       
        public LoginException(String msg, Exception e) : base(msg, e)
        { }
    }
}
